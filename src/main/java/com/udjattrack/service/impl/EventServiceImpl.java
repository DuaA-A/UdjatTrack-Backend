package com.udjattrack.service.impl;

import com.udjattrack.dto.request.EventRequest;
import com.udjattrack.dto.request.OfflineSyncRequest;
import com.udjattrack.dto.response.EventResponse;
import com.udjattrack.dto.websocket.AlertEventMessage;
import com.udjattrack.entity.Trip;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.EventRecordRepository;
import com.udjattrack.repository.TripRepository;
import com.udjattrack.service.EventService;
import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.request.CreateIncidentRequest;
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

    @Override
    @Transactional
    public EventResponse reportEvent(UUID tripId, EventRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));

        // 1. Save to Relational DB (long-term management & history)
        com.udjattrack.entity.EventRecord relationalRecord = com.udjattrack.entity.EventRecord.builder()
                .trip(trip)
                .eventType(request.eventType())
                .severity(request.severity())
                .payload(request.payload())
                .timestamp(request.timestamp() != null ? request.timestamp() : LocalDateTime.now())
                .build();
        relationalRepository.save(relationalRecord);

        boolean alertCreated = false;

        // 2. Auto-Trigger Alert for Dashboard
        if (request.severity() != null && request.severity() != com.udjattrack.entity.enums.SeverityLevel.LOW) {
            CreateAlertRequest alertReq = new CreateAlertRequest(
                    tripId,
                    AlertType.TRIP_STATE, // Or a more specific type based on event
                    request.severity(),
                    "EventRecord",
                    relationalRecord.getId(),
                    "Critical Event Detected: " + request.eventType()
            );
            alertService.createAlert(alertReq); // this also broadcasts the websocket
            alertCreated = true;
        }

        // 3. Auto-trigger Incident if it's a crash or rollover
        if ("CRASH".equalsIgnoreCase(request.eventType()) || "ROLLOVER".equalsIgnoreCase(request.eventType())) {
            CreateIncidentRequest incReq = new CreateIncidentRequest(
                    tripId,
                    IncidentType.TRAFFIC_COLLISION, // Or map specifically
                    request.severity(),
                    "Auto-detected location", // could extract from payload
                    request.payload()
            );
            emergencyService.createIncident(incReq);
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
        log.info("Syncing offline data: {} telemetry, {} events", 
                 request.telemetryRecords().size(), request.events().size());
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
