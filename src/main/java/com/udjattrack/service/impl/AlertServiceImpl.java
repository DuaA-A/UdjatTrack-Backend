package com.udjattrack.service.impl;

import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.AlertResponse;
import com.udjattrack.dto.websocket.AlertEventMessage;
import com.udjattrack.entity.Alert;
import com.udjattrack.entity.Trip;
import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.DriverState;
import com.udjattrack.entity.enums.SeverityLevel;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.*;
import com.udjattrack.service.AlertService;
import com.udjattrack.websocket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final TripRepository tripRepository;
    private final WebSocketPublisher webSocketPublisher;
    private final SOSRequestRepository sosRequestRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final IncidentRepository incidentRepository;
    private final TripStateRepository tripStateRepository;
    private final EventRecordRepository relationalRepository;

    @Override
    public AlertResponse createAlert(CreateAlertRequest request) {
        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", request.tripId()));
        
        Alert alert = Alert.builder()
                .trip(trip)
                .alertType(request.alertType())
                .severity(request.severity())
                .message(request.message())
                .alertableType(request.alertableType())
                .alertableId(request.alertableId())
                .acknowledged(false)
                .readByManager(false)
                .build();
        Alert saved = alertRepository.save(alert);
        
        // Push alert via WebSocket
        if (trip.getDriver() != null && trip.getDriver().getFleetManager() != null) {
            UUID managerId = trip.getDriver().getFleetManager().getUserId();
            webSocketPublisher.publishAlert(managerId, toMessage(saved));
        } else {
            log.warn("Could not publish WebSocket alert: Trip {} has no associated Fleet Manager", trip.getTripId());
        }
        
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertResponse getAlertById(UUID alertId) {
        return alertRepository.findById(alertId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", alertId));
    }

    @Override
    public AlertResponse acknowledgeAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", alertId));
        alert.setAcknowledged(true);
        alert.setAckedAt(java.time.LocalDateTime.now());
        return toResponse(alertRepository.save(alert));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> getUnacknowledgedAlerts(UUID fleetManagerId) {
        return alertRepository.findUnacknowledgedAlertsForFleetManager(fleetManagerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsByTrip(UUID tripId) {
        return alertRepository.findAllByTripTripIdOrderByTimestampDesc(tripId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public void evaluateEvent(TelemetryRequest request) {
        // Telemetry no longer carries event/severity data.
        // Events are evaluated and trigger Alerts exclusively via EventService.
    }

    @Override
    public void notifyFleetManager(UUID alertId) {
        log.info("Fleet manager notified for alert: {}", alertId);
    }

    private AlertResponse toResponse(Alert a) {
        return AlertResponse.builder()
                .alertId(a.getAlertId())
                .tripId(a.getTrip().getTripId())
                .alertType(a.getAlertType())
                .severity(a.getSeverity())
                .acknowledged(a.getAcknowledged())
                .readByManager(a.getReadByManager())
                .ackedAt(a.getAckedAt())
                .message(a.getMessage())
                .alertableType(a.getAlertableType())
                .alertableId(a.getAlertableId())
                .timestamp(a.getTimestamp())
                .build();
    }

    private AlertEventMessage toMessage(Alert a) {
        Trip trip = a.getTrip();
        
        Object alertableObj = fetchAlertable(a.getAlertableType(), a.getAlertableId());
        
        return AlertEventMessage.builder()
                .alertId(a.getAlertId())
                .tripId(trip.getTripId())
                .alertType(a.getAlertType())
                .severity(a.getSeverity())
                .timeStamp(a.getTimestamp())
                .acknowledged(a.getAcknowledged())
                .readByManager(a.getReadByManager())
                .ackedAt(a.getAckedAt())
                .message(a.getMessage())
                .driver(AlertEventMessage.DriverInfo.builder()
                        .driverId(trip.getDriver().getUserId())
                        .name(trip.getDriver().getName())
                        .build())
                .vehicle(AlertEventMessage.VehicleInfo.builder()
                        .plateNumber(trip.getVehicle().getPlateNumber())
                        .model(trip.getVehicle().getModel())
                        .build())
                .alertable(alertableObj)
                .build();
    }

    private Object fetchAlertable(String type, UUID id) {
        if (type == null || id == null) return null;
        
        return switch (type) {
            case "SOSRequest" -> sosRequestRepository.findById(id).orElse(null);
            case "MaintenanceRequest" -> maintenanceRequestRepository.findById(id).orElse(null);
            case "Incident" -> incidentRepository.findById(id).orElse(null);
            case "TripState" -> tripStateRepository.findById(id).orElse(null);
            case "EventRecord" -> relationalRepository.findById(id).orElse(null);
            default -> null;
        };
    }
}
