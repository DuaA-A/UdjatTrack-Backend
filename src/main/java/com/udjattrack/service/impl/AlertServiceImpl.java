package com.udjattrack.service.impl;

import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.AlertResponse;
import com.udjattrack.dto.websocket.AlertEventMessage;
import com.udjattrack.entity.Alert;
import com.udjattrack.entity.TripLog;
import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.DriverState;
import com.udjattrack.entity.enums.SeverityLevel;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.AlertRepository;
import com.udjattrack.repository.TripLogRepository;
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
    private final TripLogRepository tripLogRepository;
    private final WebSocketPublisher webSocketPublisher;

    @Override
    public AlertResponse createAlert(CreateAlertRequest request) {
        TripLog tripLog = tripLogRepository.findById(request.tripLogId())
                .orElseThrow(() -> new ResourceNotFoundException("TripLog", "id", request.tripLogId()));
        Alert alert = Alert.builder()
                .tripLog(tripLog)
                .alertType(request.alertType())
                .severity(request.severity())
                .message(request.message())
                .acknowledged(false)
                .build();
        Alert saved = alertRepository.save(alert);
        
        // Push alert via WebSocket
        UUID managerId = tripLog.getTrip().getDriver().getFleetManager().getUserId();
        webSocketPublisher.publishAlert(managerId, toMessage(saved));
        
        return toResponse(saved);
    }

    @Override
    public AlertResponse acknowledgeAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", alertId));
        alert.setAcknowledged(true);
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
    public List<AlertResponse> getAlertsByTripLog(UUID tripLogId) {
        return alertRepository.findAllByTripLogLogIdOrderByTimestampDesc(tripLogId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public void evaluateEvent(TelemetryRequest request) {
        if (request.driverState() == null) return;

        if (request.driverState() == DriverState.DROWSY || request.driverState() == DriverState.UNCONSCIOUS) {
            SeverityLevel severity = request.driverState() == DriverState.UNCONSCIOUS
                    ? SeverityLevel.CRITICAL : SeverityLevel.HIGH;

            tripLogRepository.findByTripTripId(request.tripId()).ifPresent(tripLog -> {
                Alert alert = Alert.builder()
                        .tripLog(tripLog)
                        .alertType(AlertType.FATIGUE)
                        .severity(severity)
                        .message("Driver alertness degraded: " + request.driverState())
                        .acknowledged(false)
                        .build();
                Alert saved = alertRepository.save(alert);
                
                // Publish Fatigue Alert
                UUID managerId = tripLog.getTrip().getDriver().getFleetManager().getUserId();
                webSocketPublisher.publishAlert(managerId, toMessage(saved));
                
                log.warn("FATIGUE ALERT created for trip: {} driver state: {}",
                        request.tripId(), request.driverState());
            });
        }
    }

    @Override
    public void notifyFleetManager(UUID alertId) {
        log.info("Fleet manager notified for alert: {}", alertId);
    }

    private AlertResponse toResponse(Alert a) {
        return AlertResponse.builder()
                .alertId(a.getAlertId())
                .tripLogId(a.getTripLog().getLogId())
                .alertType(a.getAlertType())
                .severity(a.getSeverity())
                .acknowledged(a.getAcknowledged())
                .message(a.getMessage())
                .timestamp(a.getTimestamp())
                .build();
    }

    private AlertEventMessage toMessage(Alert a) {
        return AlertEventMessage.builder()
                .alertId(a.getAlertId())
                .tripId(a.getTripLog().getTrip().getTripId())
                .type(a.getAlertType())
                .severity(a.getSeverity())
                .message(a.getMessage())
                .timestamp(a.getTimestamp())
                .build();
    }
}
