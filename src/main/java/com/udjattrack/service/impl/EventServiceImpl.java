package com.udjattrack.service.impl;

import com.udjattrack.dto.request.EventRequest;
import com.udjattrack.dto.request.OfflineSyncRequest;
import com.udjattrack.dto.response.EventResponse;
import com.udjattrack.entity.*;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.EventRecordRepository;
import com.udjattrack.repository.TripRepository;
import com.udjattrack.repository.TripStateRepository;
import com.udjattrack.service.EventService;
import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.IncidentType;
import com.udjattrack.service.AlertService;
import com.udjattrack.service.EmergencyService;
import com.udjattrack.websocket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRecordRepository relationalRepository;
    private final TripRepository tripRepository;
    private final WebSocketPublisher webSocketPublisher;
    private final AlertService alertService;
    private final EmergencyService emergencyService;
    private final com.udjattrack.service.TelemetryService telemetryService;
    private final TripStateRepository tripStateRepository;

    @Override
    @Transactional
    public EventResponse reportEvent(UUID tripId, EventRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));

        if (trip.getStatus() == com.udjattrack.entity.enums.TripStatus.PLANNED) {
            throw new com.udjattrack.exception.BusinessException("Cannot report events for a trip that has not started yet.");
        }

        LocalDateTime eventTime = request.timeStamp() != null ? request.timeStamp().toLocalDateTime() : LocalDateTime.now();

        if (trip.getTripLog() != null) {
            LocalDateTime start = trip.getTripLog().getActualStartTime();
            LocalDateTime end = trip.getTripLog().getActualEndTime();

            if (start != null && eventTime.isBefore(start)) {
                throw new com.udjattrack.exception.BusinessException("Event timestamp cannot be before the trip's actual start time.");
            }
            if (end != null && eventTime.isAfter(end)) {
                throw new com.udjattrack.exception.BusinessException("Event timestamp cannot be after the trip's actual end time.");
            }
        }

        // 1. Save to Relational DB (long-term management & history)
        com.udjattrack.entity.EventRecord relationalRecord = com.udjattrack.entity.EventRecord.builder()
                .trip(trip)
                .eventType(request.eventType())
                .severity(request.severity())
                .payload(request.payload())
                .timestamp(request.timeStamp() != null ? request.timeStamp().toLocalDateTime() : LocalDateTime.now())
                .build();
        relationalRepository.save(relationalRecord);

        boolean alertCreated = false;

        // 2. Scenario A: Auto-detected CRASH or ROLLOVER -> Create Incident (which triggers its own Alert)
        if ("CRASH".equalsIgnoreCase(request.eventType()) || "ROLLOVER".equalsIgnoreCase(request.eventType())) {
            try {
                emergencyService.createIncident(new com.udjattrack.dto.request.CreateIncidentRequest(
                        tripId,
                        "CRASH".equalsIgnoreCase(request.eventType()) ? IncidentType.ROAD_ACCIDENT : IncidentType.OTHER_INCIDENT,
                        request.severity(),
                        "Auto-detected location",
                        "Auto-generated incident from IoT event: " + request.eventType(),
                        request.payload()
                ));
                alertCreated = true; // Alert is handled by createIncident
            } catch (Exception e) {
                log.error("Failed to auto-trigger incident for crash event {}: {}", relationalRecord.getId(), e.getMessage());
            }
        } 
        // 3. Scenario B: All other Safety Events -> Create Alert linked directly to EventRecord
        else {
            try {
                AlertType alertType = request.eventType().toUpperCase().contains("FATIGUE") ? AlertType.FATIGUE : AlertType.INCIDENT;
                
                com.udjattrack.dto.request.CreateAlertRequest alertReq = new com.udjattrack.dto.request.CreateAlertRequest(
                        tripId,
                        alertType,
                        request.severity(),
                        "EventRecord",
                        relationalRecord.getId(),
                        "Safety Event Detected: " + request.eventType()
                );
                alertService.createAlert(alertReq);
                alertCreated = true;
            } catch (Exception e) {
                log.error("Failed to auto-trigger alert for safety event {}: {}", relationalRecord.getId(), e.getMessage());
            }
        }

        return toResponse(relationalRecord, alertCreated);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<EventResponse> getEventsByTrip(UUID tripId) {
        return relationalRepository.findAllByTripTripIdOrderByTimestampDesc(tripId)
                .stream().map(e -> toResponse(e, false)).collect(java.util.stream.Collectors.toList());
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
    public java.util.List<EventResponse> getAllEvents(UUID tripId, String eventType) {
        if (tripId != null && eventType != null) {
            return relationalRepository.findAllByTripTripIdAndEventTypeOrderByTimestampDesc(tripId, eventType)
                    .stream().map(e -> toResponse(e, false)).toList();
        } else if (tripId != null) {
            return getEventsByTrip(tripId);
        }
        return relationalRepository.findAll().stream()
                .map(e -> toResponse(e, false)).toList();
    }

    @Override
    @Transactional
    public void syncOfflineData(OfflineSyncRequest request) {
        log.info("Syncing offline data for trip {}: {} telemetry, {} events", 
                 request.tripId(),
                 request.telemetryRecords() != null ? request.telemetryRecords().size() : 0, 
                 request.events() != null ? request.events().size() : 0);
                 
        if (request.telemetryRecords() != null && !request.telemetryRecords().isEmpty()) {
            request.telemetryRecords().forEach(telemetry -> {
                try {
                    telemetryService.ingestTelemetry(telemetry);
                } catch (Exception e) {
                    log.error("Failed to sync offline telemetry record: {}", e.getMessage());
                }
            });
        }
        
        if (request.events() != null && !request.events().isEmpty()) {
            request.events().forEach(event -> {
                try {
                    reportEvent(request.tripId(), event);
                } catch (Exception e) {
                    log.error("Failed to sync offline event record: {}", e.getMessage());
                }
            });
        }
    }

    private EventResponse toResponse(com.udjattrack.entity.EventRecord record, boolean alertCreated) {
        return EventResponse.builder()
                .eventId(record.getId())
                .tripId(record.getTrip().getTripId())
                .eventType(record.getEventType())
                .severity(record.getSeverity())
                .payload(record.getPayload())
                .timestamp(record.getTimestamp())
                .alertCreated(alertCreated)
                .build();
    }
}
