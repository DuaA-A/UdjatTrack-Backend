package com.udjattrack.service.impl;

import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.request.CreateIncidentRequest;
import com.udjattrack.dto.request.EventRequest;
import com.udjattrack.dto.request.OfflineSyncRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.EventResponse;
import com.udjattrack.entity.*;
import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.IncidentType;
import com.udjattrack.entity.enums.SeverityLevel;
import com.udjattrack.entity.enums.TripStatus;
import com.udjattrack.exception.BusinessException;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.EventRecordRepository;
import com.udjattrack.repository.TripRepository;
import com.udjattrack.repository.TripStateRepository;
import com.udjattrack.service.AlertService;
import com.udjattrack.service.EmergencyService;
import com.udjattrack.service.EventService;
import com.udjattrack.service.TelemetryService;
import com.udjattrack.websocket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRecordRepository relationalRepository;
    private final TripRepository tripRepository;
    private final WebSocketPublisher webSocketPublisher;
    private final AlertService alertService;
    private final EmergencyService emergencyService;
    private final TelemetryService telemetryService;
    private final TripStateRepository tripStateRepository;
    
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private EventService self;

    @Override
    @Transactional
    public EventResponse reportEvent(UUID tripId, EventRequest request) {
        if (tripId == null) throw new BusinessException("Trip ID is required");
        if (request == null) throw new BusinessException("Event request is required");

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));

        if (trip.getStatus() == TripStatus.PLANNED) {
            throw new BusinessException("Cannot report events for a trip that has not started yet.");
        }

        LocalDateTime eventTime = request.timeStamp() != null ? request.timeStamp().toLocalDateTime() : LocalDateTime.now();

        if (trip.getTripLog() != null) {
            LocalDateTime start = trip.getTripLog().getActualStartTime();
            LocalDateTime end = trip.getTripLog().getActualEndTime();

            if (start != null && eventTime.isBefore(start)) {
                throw new BusinessException("Event timestamp cannot be before the trip's actual start time.");
            }
            if (end != null && eventTime.isAfter(end)) {
                throw new BusinessException("Event timestamp cannot be after the trip's actual end time.");
            }
        }

        SeverityLevel severity = determineSeverity(request.eventType());

        // 1. Save to Relational DB (long-term management & history)
        EventRecord relationalRecord = EventRecord.builder()
                .trip(trip)
                .eventType(request.eventType())
                .severity(severity)
                .payload(request.payload())
                .timestamp(eventTime)
                .build();
        
        Objects.requireNonNull(relationalRecord);
        EventRecord savedRecord = relationalRepository.saveAndFlush(relationalRecord);

        boolean alertCreated = false;

        // 2. Scenario A: Auto-detected CRASH or ROLLOVER -> Create Incident (which triggers its own Alert)
        if ("CRASH".equalsIgnoreCase(request.eventType()) || "ROLLOVER".equalsIgnoreCase(request.eventType())) {
            emergencyService.createIncident(new CreateIncidentRequest(
                    tripId,
                    "CRASH".equalsIgnoreCase(request.eventType()) ? IncidentType.ROAD_ACCIDENT : IncidentType.OTHER_INCIDENT,
                    severity,
                    "Auto-detected location",
                    request.payload()
            ));
            alertCreated = true; // Alert is handled by createIncident
        } 
        // 3. Scenario B: All other Safety Events -> Create Alert linked directly to EventRecord
        else {
            AlertType alertType = request.eventType().toUpperCase().contains("FATIGUE") ? AlertType.FATIGUE : AlertType.INCIDENT;
            
            CreateAlertRequest alertReq = new CreateAlertRequest(
                    tripId,
                    alertType,
                    severity,
                    "EventRecord",
                    savedRecord.getId(),
                    "Safety Event Detected: " + request.eventType()
            );
            alertService.createAlert(alertReq);
            alertCreated = true;
        }

        return toResponse(savedRecord, alertCreated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByTrip(UUID tripId) {
        return relationalRepository.findAllByTripTripIdOrderByTimestampDesc(tripId)
                .stream().map(e -> toResponse(e, false)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID eventId) {
        return relationalRepository.findById(eventId)
                .map(e -> toResponse(e, false))
                .orElseThrow(() -> new ResourceNotFoundException("EventRecord", "id", eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(UUID tripId, String eventType) {
        if (tripId != null && eventType != null) {
            return relationalRepository.findAllByTripTripIdAndEventTypeOrderByTimestampDesc(tripId, eventType)
                    .stream().map(e -> toResponse(e, false)).collect(Collectors.toList());
        } else if (tripId != null) {
            return getEventsByTrip(tripId);
        }
        return relationalRepository.findAll().stream()
                .map(e -> toResponse(e, false)).collect(Collectors.toList());
    }

    @Override
    public void syncOfflineData(OfflineSyncRequest request) {
        if (request == null) return;
        
        UUID tripId = request.tripId();
        if (tripId == null) {
            log.warn("Cannot sync offline data: tripId is null");
            return;
        }

        log.info("Syncing offline data for trip {}: {} telemetry, {} events", 
                 tripId,
                 request.telemetryRecords() != null ? request.telemetryRecords().size() : 0, 
                 request.events() != null ? request.events().size() : 0);
                 
        if (request.telemetryRecords() != null && !request.telemetryRecords().isEmpty()) {
            request.telemetryRecords().forEach(telemetry -> {
                try {
                    telemetryService.ingestTelemetry(tripId, telemetry);
                } catch (Exception e) {
                    log.error("Failed to sync offline telemetry record for trip {}: {}", tripId, e.getMessage());
                }
            });
        }
        
        if (request.events() != null && !request.events().isEmpty()) {
            request.events().forEach(event -> {
                try {
                    self.reportEvent(tripId, event);
                } catch (Exception e) {
                    log.error("Failed to sync offline event record for trip {}: {}. Error details: ", 
                             tripId, e.getMessage(), e);
                }
            });
        }
    }

    private EventResponse toResponse(EventRecord record, boolean alertCreated) {
        if (record == null) return null;
        
        return EventResponse.builder()
                .eventId(record.getId())
                .tripId(record.getTrip() != null ? record.getTrip().getTripId() : null)
                .eventType(record.getEventType())
                .severity(record.getSeverity())
                .payload(record.getPayload())
                .timestamp(record.getTimestamp())
                .alertCreated(alertCreated)
                .build();
    }
    private SeverityLevel determineSeverity(String eventType) {
        if (eventType == null) return SeverityLevel.MEDIUM;
        String type = eventType.toUpperCase();
        if (type.contains("CRASH") || type.contains("ROLLOVER")) return SeverityLevel.CRITICAL;
        if (type.contains("FATIGUE") || type.contains("SOS")) return SeverityLevel.HIGH;
        return SeverityLevel.MEDIUM;
    }
}
