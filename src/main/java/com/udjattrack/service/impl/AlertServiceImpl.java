package com.udjattrack.service.impl;

import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.AlertResponse;
import com.udjattrack.dto.response.AlertSummaryResponse;
import com.udjattrack.entity.*;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.*;
import com.udjattrack.dto.websocket.AlertEventMessage;
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
    private final com.udjattrack.service.NotificationService notificationService;
    private final com.udjattrack.service.EmailService emailService;

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
                .timestamp(java.time.LocalDateTime.now())
                .build();
        Alert saved = alertRepository.save(alert);
        
        // Push alert via WebSocket AFTER transaction commit
        if (trip.getDriver() != null) {
            // Save notification for Driver History
            notificationService.sendAlertTriggeredNotification(trip.getDriver(), saved.getAlertId(), saved.getMessage());
            
            if (trip.getDriver().getFleetManager() != null) {
                FleetManager manager = trip.getDriver().getFleetManager();
                UUID fleetId = manager.getUserId();

                // Save notification for Manager History
                notificationService.sendAlertTriggeredNotification(manager, saved.getAlertId(), saved.getMessage());
                
                // SEND REAL-TIME EMAIL TO MANAGER
                emailService.sendAlertNotification(manager.getEmail(), manager.getName(), saved);
                
                AlertEventMessage message = toMessage(saved);
                
                if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                    org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                webSocketPublisher.publishAlert(fleetId, message);
                            }
                        }
                    );
                } else {
                    webSocketPublisher.publishAlert(fleetId, message);
                }
            }
        } else {
            log.warn("Could not publish WebSocket alert: Trip {} has no associated Driver", trip.getTripId());
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
        alert.setAcknowledgedAt(java.time.LocalDateTime.now());
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
        Trip trip = a.getTrip();
        AlertResponse.DriverInfo driverInfo = null;
        if (trip.getDriver() != null) {
            driverInfo = AlertResponse.DriverInfo.builder()
                    .driverId(trip.getDriver().getUserId())
                    .name(trip.getDriver().getName())
                    .phone(trip.getDriver().getPhoneNumber()) // Adjust getter name based on your User entity
                    .currentState(Boolean.TRUE.equals(trip.getDriver().getIdle()) ? "IDLE" : "ACTIVE") 
                    .build();
        }
        AlertResponse.VehicleInfo vehicleInfo = null;
        if (trip.getVehicle() != null) {
            vehicleInfo = AlertResponse.VehicleInfo.builder()
                    .vehicleId(trip.getVehicle().getVehicleId())
                    .plateNumber(trip.getVehicle().getPlateNumber())
                    .model(trip.getVehicle().getModel())
                    .build();
        }

        AlertResponse.TripInfo tripInfo = null;
        if (trip != null) {
            tripInfo = AlertResponse.TripInfo.builder()
                    .tripId(trip.getTripId())
                    .status(trip.getStatus() != null ? trip.getStatus().name() : null)
                    .scheduledStartTime(trip.getScheduledStartTime())
                    .scheduledEndTime(trip.getScheduledEndTime())
                    .source(trip.getSource())
                    .destination(trip.getDestination())
                    .build();
        }

        java.util.Map<String, Object> eventDetails = new java.util.HashMap<>();
        if ("TripState".equalsIgnoreCase(a.getAlertableType())) {
             tripStateRepository.findById(a.getAlertableId()).ifPresent(s -> {
                 eventDetails.put("eventId", s.getStateId());
                 eventDetails.put("eventType", a.getAlertType());
                 eventDetails.put("timestamp", s.getLastUpdatedAt());
             });
        }
        return AlertResponse.builder()
                .alertId(a.getAlertId())
                .tripId(a.getTrip().getTripId())
                .alertType(a.getAlertType())
                .severity(a.getSeverity())
                .acknowledged(a.getAcknowledged())
                .acknowledgedAt(a.getAcknowledgedAt())
                .message(a.getMessage())
                .alertableType(a.getAlertableType())
                .alertableId(a.getAlertableId())
                .timestamp(a.getTimestamp())
                .trip(tripInfo)
                .driver(driverInfo)
                .vehicle(vehicleInfo)
                .event(eventDetails.isEmpty() ? null : eventDetails)
                .build();
    }
    private AlertEventMessage toMessage(Alert a) {
        Trip trip = a.getTrip();
        
        // Map alert type string
        String typeStr = switch (a.getAlertType()) {
            case SOS_REQ -> "SOSReq";
            case MAINTENANCE -> "Maintenance";
            case INCIDENT -> "Incident";
            case FATIGUE, TRIP_STATE -> "Fatigue";
            default -> a.getAlertType().name();
        };

        // Capitalize Severity
        String severityStr = a.getSeverity().name().charAt(0) + a.getSeverity().name().substring(1).toLowerCase();

        // Fetch alertable details
        java.util.Map<String, Object> alertableDetails = new java.util.HashMap<>();
        if ("SOSRequest".equalsIgnoreCase(a.getAlertableType())) {
            sosRequestRepository.findById(a.getAlertableId()).ifPresent(sos -> {
                alertableDetails.put("type", "SOSRequest");
                alertableDetails.put("id", sos.getIssueId());
                alertableDetails.put("triggeredAt", sos.getTriggeredAt());
                alertableDetails.put("status", sos.getStatus());
                alertableDetails.put("category", "SOS");
                alertableDetails.put("triggeredAt", sos.getTriggeredAt());
                alertableDetails.put("payload", sos.getPayload());
            });
        } else if ("MaintenanceRequest".equalsIgnoreCase(a.getAlertableType())) {
            maintenanceRequestRepository.findById(a.getAlertableId()).ifPresent(m -> {
                alertableDetails.put("type", "MaintenanceRequest");
                alertableDetails.put("id", m.getIssueId());
                alertableDetails.put("triggeredAt", m.getTriggeredAt());
                alertableDetails.put("status", m.getStatus());
                alertableDetails.put("category", "ENGINE"); // Placeholder or mapped
                alertableDetails.put("triggeredAt", m.getTriggeredAt());
                alertableDetails.put("maintenanceType", m.getMaintenanceType());
                alertableDetails.put("payload", m.getPayload());
            });
        } else if ("Incident".equalsIgnoreCase(a.getAlertableType())) {
            incidentRepository.findById(a.getAlertableId()).ifPresent(i -> {
                alertableDetails.put("type", "Incident");
                alertableDetails.put("incidentId", i.getIssueId());
                alertableDetails.put("incidentType", i.getType());
                alertableDetails.put("severity", i.getSeverity().name().charAt(0) + i.getSeverity().name().substring(1).toLowerCase());
                // Extract description from payload if present
                String desc = "Manual Incident Report";
                if (i.getPayload() != null && i.getPayload().containsKey("description")) {
                    desc = String.valueOf(i.getPayload().get("description"));
                } else if (a.getMessage() != null && a.getMessage().contains("AUTO-DETECT")) {
                    desc = "Auto-generated incident from IoT sensor detection.";
                }
                alertableDetails.put("description", desc);
            });
        } else if ("TripState".equalsIgnoreCase(a.getAlertableType())) {
            tripStateRepository.findById(a.getAlertableId()).ifPresent(s -> {
                alertableDetails.put("type", "TripState");
                alertableDetails.put("tripStateId", s.getStateId());
                alertableDetails.put("driverState", s.getDriverState().name().charAt(0) + s.getDriverState().name().substring(1).toLowerCase());
                alertableDetails.put("latitude", s.getLatitude());
                alertableDetails.put("longitude", s.getLongitude());
                alertableDetails.put("lastUpdatedAt", s.getLastUpdatedAt());
            });
        }

        return AlertEventMessage.builder()
                .alertId(a.getAlertId())
                .tripId(trip.getTripId())
                .alertType(typeStr)
                .severity(severityStr)
                .timeStamp(a.getTimestamp().atOffset(java.time.ZoneOffset.UTC))
                .acknowledged(a.getAcknowledged())
                .message(a.getMessage())
                .driver(trip.getDriver() != null ? AlertEventMessage.DriverInfo.builder()
                        .driverId(trip.getDriver().getUserId())
                        .name(trip.getDriver().getName())
                        .build() : null)
                .vehicle(trip.getVehicle() != null ? AlertEventMessage.VehicleInfo.builder()
                        .plateNumber(trip.getVehicle().getPlateNumber())
                        .model(trip.getVehicle().getModel())
                        .build() : null)
                .alertable(alertableDetails)
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> getAllAlertsByFleetManager(UUID fleetManagerId) {
        return alertRepository.findAllByFleetManager(fleetManagerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> getAcknowledgedAlerts(UUID fleetManagerId) {
        return alertRepository.findAcknowledgedAlertsForFleetManager(fleetManagerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AlertSummaryResponse getAlertSummary(UUID fleetManagerId) {
        List<Alert> allAlerts = alertRepository.findAllByFleetManager(fleetManagerId);
        long total = allAlerts.size();
        long acknowledged = allAlerts.stream().filter(a -> Boolean.TRUE.equals(a.getAcknowledged())).count();
        long unacknowledged = total - acknowledged;
        long critical = allAlerts.stream().filter(a -> a.getSeverity() == com.udjattrack.entity.enums.SeverityLevel.CRITICAL).count();
        long high = allAlerts.stream().filter(a -> a.getSeverity() == com.udjattrack.entity.enums.SeverityLevel.HIGH).count();
        long medium = allAlerts.stream().filter(a -> a.getSeverity() == com.udjattrack.entity.enums.SeverityLevel.MEDIUM).count();
        long low = allAlerts.stream().filter(a -> a.getSeverity() == com.udjattrack.entity.enums.SeverityLevel.LOW).count();

        return AlertSummaryResponse.builder()
                .totalAlerts(total)
                .acknowledgedCount(acknowledged)
                .unacknowledgedCount(unacknowledged)
                .criticalCount(critical)
                .highCount(high)
                .mediumCount(medium)
                .lowCount(low)
                .build();
    }
}
