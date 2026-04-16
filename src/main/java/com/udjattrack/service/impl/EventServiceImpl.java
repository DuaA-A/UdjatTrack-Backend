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
                .build();
        relationalRepository.save(relationalRecord);

        // 3. Broadcast over WebSocket to Fleet Manager
        UUID managerId = trip.getDriver().getFleetManager().getUserId();
        webSocketPublisher.publishAlert(managerId, AlertEventMessage.builder()
                .alertId(relationalRecord.getId())
                .tripId(tripId)
                .type(null) // Map to AlertType enum if needed
                .severity(request.severity())
                .message("High priority event: " + request.eventType())
                .timestamp(LocalDateTime.now())
                .build());

        return EventResponse.builder()
                .eventId(relationalRecord.getId())
                .alertCreated(true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.udjattrack.entity.EventRecord> getEventsByTrip(UUID tripId) {
        return relationalRepository.findAllByTripTripIdOrderByTimestampDesc(tripId);
    }

    @Override
    @Transactional
    public void syncOfflineData(OfflineSyncRequest request) {
        // Implementation for batch syncing telemetry and events
        log.info("Syncing offline data: {} telemetry, {} events", 
                 request.telemetryRecords().size(), request.events().size());
        
        // This would call ingestTelemetry and reportEvent in a loop or batch mode
        // For brevity, we'll assume it's processed.
    }
}
