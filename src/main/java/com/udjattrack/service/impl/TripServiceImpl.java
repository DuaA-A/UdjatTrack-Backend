package com.udjattrack.service.impl;

import com.udjattrack.dto.request.CreateTripRequest;
import com.udjattrack.dto.response.TripLogResponse;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.dto.response.TripTimelineResponse;
import com.udjattrack.entity.*;
import com.udjattrack.entity.enums.TripProgressState;
import com.udjattrack.entity.enums.TripStatus;
import com.udjattrack.exception.BusinessException;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.*;
import com.udjattrack.service.NotificationService;
import com.udjattrack.service.TripService;
import com.udjattrack.service.AlertService;
import com.udjattrack.websocket.WebSocketPublisher;
import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.SeverityLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final TripStateRepository tripStateRepository;
    private final TripLogRepository tripLogRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationService notificationService;
    private final AlertService alertService;
    private final WebSocketPublisher webSocketPublisher;
    private final EventRecordRepository eventRecordRepository;
    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;

    @Override
    public TripResponse createTrip(CreateTripRequest request) {
        Driver driver = driverRepository.findById(request.driverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", request.driverId()));
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", request.vehicleId()));

        if (Boolean.FALSE.equals(driver.getIdle())) {
            throw new BusinessException("Driver is already assigned to an active trip");
        }
        if (Boolean.FALSE.equals(vehicle.getIdle())) {
            throw new BusinessException("Vehicle is already in use on another trip");
        }

        Trip trip = Trip.builder()
                .driver(driver).vehicle(vehicle)
                .source(request.source()).destination(request.destination())
                .scheduledStartTime(request.scheduledStartTime())
                .scheduledEndTime(request.scheduledEndTime())
                .status(TripStatus.PLANNED)
                .build();
        Trip saved = tripRepository.save(trip);

        // Create initial TripState snapshot
        TripState state = TripState.builder()
                .trip(saved)
                .build();
        tripStateRepository.save(state);

        // Notify driver
        notificationService.sendTripAssignedNotification(driver, saved);

        log.info("Trip created: {} for driver: {}", saved.getTripId(), driver.getUserId());
        return toResponse(saved);
    }

    @Override
    public TripResponse assignDriver(UUID tripId, UUID driverId) {
        Trip trip = findTripOrThrow(tripId);
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));
        trip.setDriver(driver);
        notificationService.sendTripAssignedNotification(driver, trip);
        return toResponse(tripRepository.save(trip));
    }

    @Override
    public TripResponse assignVehicle(UUID tripId, UUID vehicleId) {
        Trip trip = findTripOrThrow(tripId);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));
        trip.setVehicle(vehicle);
        return toResponse(tripRepository.save(trip));
    }

    @Override
    public TripResponse startTrip(UUID tripId) {
        Trip trip = findTripOrThrow(tripId);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new BusinessException("Only PLANNED trips can be started");
        }

        trip.setStatus(TripStatus.ONGOING);
        updateTripProgressState(trip, TripProgressState.STARTED, null);

        // Create trip log
        TripLog log = TripLog.builder().trip(trip).actualStartTime(LocalDateTime.now()).build();
        tripLogRepository.save(log);

        // Mark driver and vehicle as not idle
        trip.getDriver().setIdle(false);
        trip.getVehicle().setIdle(false);
        driverRepository.save(trip.getDriver());
        vehicleRepository.save(trip.getVehicle());

        return toResponse(tripRepository.save(trip));
    }

    @Override
    public TripResponse stopTrip(UUID tripId, com.udjattrack.dto.request.LocationDTO location) {
        Trip trip = findTripOrThrow(tripId);
        if (trip.getStatus() != TripStatus.ONGOING) {
            throw new BusinessException("Only ONGOING trips can be stopped (paused)");
        }
        trip.setStatus(TripStatus.ON_BREAK);
        updateTripProgressState(trip, TripProgressState.PAUSED, location);
        
        CreateAlertRequest alertReq = new CreateAlertRequest(
                tripId, AlertType.TRIP_STATE, SeverityLevel.MEDIUM,
                "TripState", trip.getTripId(), "Trip paused for break"
        );
        alertService.createAlert(alertReq);

        return toResponse(tripRepository.save(trip));
    }

    @Override
    public TripResponse resumeTrip(UUID tripId, com.udjattrack.dto.request.LocationDTO location) {
        Trip trip = findTripOrThrow(tripId);
        if (trip.getStatus() != TripStatus.ON_BREAK) {
            throw new BusinessException("Only ON_BREAK trips can be resumed");
        }
        trip.setStatus(TripStatus.ONGOING);
        updateTripProgressState(trip, TripProgressState.RESUMED, location);

        CreateAlertRequest alertReq = new CreateAlertRequest(
                tripId, AlertType.TRIP_STATE, SeverityLevel.LOW,
                "TripState", trip.getTripId(), "Trip resumed from break"
        );
        alertService.createAlert(alertReq);

        return toResponse(tripRepository.save(trip));
    }

    @Override
    public TripResponse completeTrip(UUID tripId) {
        Trip trip = findTripOrThrow(tripId);
        trip.setStatus(TripStatus.FINISHED);
        updateTripProgressState(trip, TripProgressState.COMPLETED, null);

        // Finalize trip log
        tripLogRepository.findByTripTripId(tripId).ifPresent(tl -> {
            tl.setActualEndTime(LocalDateTime.now());
            if (tl.getActualStartTime() != null) {
                tl.setTotalDuration((double) Duration.between(
                        tl.getActualStartTime(), tl.getActualEndTime()).toMinutes());
            }
            tripLogRepository.save(tl);
        });

        // Free driver and vehicle
        trip.getDriver().setIdle(true);
        trip.getVehicle().setIdle(true);
        driverRepository.save(trip.getDriver());
        vehicleRepository.save(trip.getVehicle());

        return toResponse(tripRepository.save(trip));
    }

    @Override
    public TripResponse cancelTrip(UUID tripId) {
        Trip trip = findTripOrThrow(tripId);
        trip.setStatus(TripStatus.CANCELLED);
        trip.getDriver().setIdle(true);
        trip.getVehicle().setIdle(true);
        driverRepository.save(trip.getDriver());
        vehicleRepository.save(trip.getVehicle());
        return toResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional(readOnly = true)
    public TripStateResponse trackTripProgress(UUID tripId) {
        TripState state = tripStateRepository.findByTripTripId(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("TripState", "tripId", tripId));
        return toStateResponse(state);
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getTripById(UUID tripId) {
        return toResponse(findTripOrThrow(tripId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByDriver(UUID driverId, String status) {
        if (status == null || status.isBlank()) {
            return tripRepository.findAllByDriverUserIdOrderByCreatedAtDesc(driverId)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        List<TripStatus> targetStatuses = mapTimeframeToStatuses(status);
        List<Trip> trips = tripRepository.findAllByDriverUserIdAndStatusInOrderByCreatedAtDesc(driverId, targetStatuses);

        if ("upcoming".equalsIgnoreCase(status)) {
            LocalDateTime now = LocalDateTime.now();
            return trips.stream()
                    .filter(t -> t.getScheduledStartTime() != null && t.getScheduledStartTime().isAfter(now))
                    .map(this::toResponse).collect(Collectors.toList());
        }

        return trips.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByFleetManager(UUID managerId) {
        return tripRepository.findAllByFleetManager(managerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByFleetManagerWithFilters(UUID managerId, String status, UUID vehicleId, String dateFrom, String dateTo) {
        List<TripStatus> targetStatuses = mapTimeframeToStatuses(status);

        // Start with all trips by fleet manager, then filter
        List<Trip> trips = tripRepository.findAllByFleetManager(managerId);

        return trips.stream()
                .filter(t -> targetStatuses.contains(t.getStatus()))
                .filter(t -> {
                    if ("upcoming".equalsIgnoreCase(status)) {
                        return t.getScheduledStartTime() != null && t.getScheduledStartTime().isAfter(LocalDateTime.now());
                    }
                    return true;
                })
                .filter(t -> vehicleId == null || t.getVehicle().getVehicleId().equals(vehicleId))
                .filter(t -> {
                    if (dateFrom == null) return true;
                    try {
                        LocalDateTime from = LocalDateTime.parse(dateFrom);
                        return t.getCreatedAt() != null && !t.getCreatedAt().isBefore(from);
                    } catch (DateTimeParseException e) {
                        return true;
                    }
                })
                .filter(t -> {
                    if (dateTo == null) return true;
                    try {
                        LocalDateTime to = LocalDateTime.parse(dateTo);
                        return t.getCreatedAt() != null && !t.getCreatedAt().isAfter(to);
                    } catch (DateTimeParseException e) {
                        return true;
                    }
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripStateResponse> getActiveTripStates(UUID managerId) {
        List<TripStatus> activeStatuses = List.of(TripStatus.ONGOING, TripStatus.ON_BREAK);
        return tripRepository.findAllByFleetManager(managerId).stream()
                .filter(t -> activeStatuses.contains(t.getStatus()))
                .map(trip -> tripStateRepository.findByTripTripId(trip.getTripId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toStateResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TripTimelineResponse getTripTimeline(UUID tripId) {
        Trip trip = findTripOrThrow(tripId);

        // Get trip log for start/end times
        TripLog tripLog = tripLogRepository.findByTripTripId(tripId).orElse(null);
        UUID tripLogId = tripLog != null ? tripLog.getLogId() : null;
        LocalDateTime actualStartTime = tripLog != null ? tripLog.getActualStartTime() : null;
        LocalDateTime actualEndTime = tripLog != null ? tripLog.getActualEndTime() : null;
        Double totalDurationHours = null;
        if (tripLog != null && tripLog.getTotalDuration() != null) {
            totalDurationHours = tripLog.getTotalDuration() / 60.0;
        }

        List<TripTimelineResponse.TimelineItem> timeline = new ArrayList<>();

        // Add event records to timeline
        List<EventRecord> events = eventRecordRepository.findAllByTripTripIdOrderByTimestampDesc(tripId);
        for (EventRecord event : events) {
            // Check if this event has an associated alert
            TripTimelineResponse.AlertDetails alertDetails = null;
            boolean isAlert = false;
            List<Alert> alerts = alertRepository.findAllByTripTripIdOrderByTimestampDesc(tripId);
            for (Alert alert : alerts) {
                if ("EventRecord".equals(alert.getAlertableType()) && alert.getAlertableId().equals(event.getId())) {
                    isAlert = true;
                    alertDetails = TripTimelineResponse.AlertDetails.builder()
                            .alertId(alert.getAlertId())
                            .severity(alert.getSeverity().name())
                            .acknowledged(alert.getAcknowledged())
                            .build();
                    break;
                }
            }

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("eventId", event.getId());
            details.put("eventType", event.getEventType());
            if (event.getPayload() != null) {
                details.put("payload", event.getPayload());
            }

            timeline.add(TripTimelineResponse.TimelineItem.builder()
                    .timestamp(event.getTimestamp())
                    .itemType("EVENT")
                    .isAlert(isAlert)
                    .alertDetails(alertDetails)
                    .details(details)
                    .build());
        }

        // Add incidents to timeline
        List<Incident> incidents = incidentRepository.findAllByTripTripIdOrderByReportedAtDesc(tripId);
        for (Incident incident : incidents) {
            TripTimelineResponse.AlertDetails alertDetails = null;
            boolean isAlert = false;
            List<Alert> alerts = alertRepository.findAllByTripTripIdOrderByTimestampDesc(tripId);
            for (Alert alert : alerts) {
                if ("Incident".equals(alert.getAlertableType()) && alert.getAlertableId().equals(incident.getIssueId())) {
                    isAlert = true;
                    alertDetails = TripTimelineResponse.AlertDetails.builder()
                            .alertId(alert.getAlertId())
                            .severity(alert.getSeverity().name())
                            .acknowledged(alert.getAcknowledged())
                            .build();
                    break;
                }
            }

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("incidentId", incident.getIssueId());
            details.put("incidentType", incident.getType());
            details.put("severity", incident.getSeverity());
            details.put("location", incident.getLocation());
            details.put("reportedAt", incident.getReportedAt());

            timeline.add(TripTimelineResponse.TimelineItem.builder()
                    .timestamp(incident.getTriggeredAt())
                    .itemType("INCIDENT")
                    .isAlert(isAlert)
                    .alertDetails(alertDetails)
                    .details(details)
                    .build());
        }

        // Sort all timeline items by timestamp ascending
        timeline.sort(Comparator.comparing(TripTimelineResponse.TimelineItem::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return TripTimelineResponse.builder()
                .tripLogId(tripLogId)
                .tripId(tripId)
                .actualStartTime(actualStartTime)
                .actualEndTime(actualEndTime)
                .totalDurationHours(totalDurationHours)
                .timeline(timeline)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getTripReport(UUID tripId) {
        Trip trip = findTripOrThrow(tripId);
        return toResponse(trip);
    }

    // ===== Private helpers =====

    private Trip findTripOrThrow(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
    }

    private List<TripStatus> mapTimeframeToStatuses(String status) {
        return switch (status.toLowerCase()) {
            case "active" -> List.of(TripStatus.ONGOING, TripStatus.ON_BREAK);
            case "past" -> List.of(TripStatus.FINISHED, TripStatus.CANCELLED);
            case "upcoming" -> List.of(TripStatus.PLANNED);
            case "ongoing" -> List.of(TripStatus.ONGOING);
            case "on_break" -> List.of(TripStatus.ON_BREAK);
            case "planned" -> List.of(TripStatus.PLANNED);
            case "finished" -> List.of(TripStatus.FINISHED);
            case "cancelled" -> List.of(TripStatus.CANCELLED);
            default -> throw new BusinessException("Invalid trip status filter. Use: active, past, upcoming, ongoing, on_break, planned, finished, cancelled");
        };
    }

    private void updateTripProgressState(Trip trip, TripProgressState state, com.udjattrack.dto.request.LocationDTO location) {
        tripStateRepository.findByTripTripId(trip.getTripId()).ifPresent(ts -> {
            ts.setTripProgressState(state);
            if (location != null) {
                ts.setLatitude(String.valueOf(location.lat()));
                ts.setLongitude(String.valueOf(location.lng()));
            }
            tripStateRepository.save(ts);
        });
    }

    private TripResponse toResponse(Trip trip) {
        TripLogResponse logResponse = null;
        LocalDateTime actualStartTime = null;
        LocalDateTime actualEndTime = null;
        if (trip.getTripLog() != null) {
            TripLog tl = trip.getTripLog();
            actualStartTime = tl.getActualStartTime();
            actualEndTime = tl.getActualEndTime();
            logResponse = TripLogResponse.builder()
                    .logId(tl.getLogId()).tripId(trip.getTripId())
                    .actualStartTime(tl.getActualStartTime())
                    .actualEndTime(tl.getActualEndTime())
                    .totalDuration(tl.getTotalDuration())
                    .totalBreakTime(tl.getTotalBreakTime())
                    .build();
        }

        // Calculate transient properties
        String title = trip.getSource() + " \u2192 " + trip.getDestination();
        
        Double expectedDurationHours = null;
        if (trip.getScheduledStartTime() != null && trip.getScheduledEndTime() != null) {
            expectedDurationHours = (double) Duration.between(trip.getScheduledStartTime(), trip.getScheduledEndTime()).toMinutes() / 60.0;
        }

        LocalDateTime estimatedArrivalTime = null;
        if (actualStartTime != null && expectedDurationHours != null) {
            estimatedArrivalTime = actualStartTime.plusMinutes((long)(expectedDurationHours * 60));
        }

        Integer progressPct = 0;
        if (trip.getStatus() == TripStatus.FINISHED) {
            progressPct = 100;
        } else if (trip.getStatus() == TripStatus.ONGOING && actualStartTime != null && expectedDurationHours != null) {
            long elapsedMinutes = Duration.between(actualStartTime, LocalDateTime.now()).toMinutes();
            progressPct = (int) Math.min(100, Math.max(0, (elapsedMinutes / (expectedDurationHours * 60.0)) * 100));
        }

        // Build TripState response if available
        TripStateResponse tripStateResponse = null;
        try {
            tripStateRepository.findByTripTripId(trip.getTripId()).ifPresent(ts -> {});
            TripState ts = tripStateRepository.findByTripTripId(trip.getTripId()).orElse(null);
            if (ts != null) {
                tripStateResponse = toStateResponse(ts);
            }
        } catch (Exception e) {
            log.debug("Could not load TripState for trip {}: {}", trip.getTripId(), e.getMessage());
        }

        return TripResponse.builder()
                .tripId(trip.getTripId())
                .driverId(trip.getDriver().getUserId())
                .driverName(trip.getDriver().getName())
                .vehicleId(trip.getVehicle().getVehicleId())
                .vehiclePlate(trip.getVehicle().getPlateNumber())
                .source(trip.getSource()).destination(trip.getDestination())
                .scheduledStartTime(trip.getScheduledStartTime())
                .scheduledEndTime(trip.getScheduledEndTime())
                .status(trip.getStatus())
                .createdAt(trip.getCreatedAt())
                .tripLog(logResponse)
                .title(title)
                .expectedDurationHours(expectedDurationHours)
                .estimatedArrivalTime(estimatedArrivalTime)
                .progressPct(progressPct)
                .actualStartTime(actualStartTime)
                .actualEndTime(actualEndTime)
                .tripState(tripStateResponse)
                .vehicle(TripResponse.VehicleInfo.builder()
                        .plateNumber(trip.getVehicle().getPlateNumber())
                        .model(trip.getVehicle().getModel())
                        .build())
                .build();
    }

    private TripStateResponse toStateResponse(TripState state) {
        return TripStateResponse.builder()
                .stateId(state.getStateId()).tripId(state.getTrip().getTripId())
                .driverState(state.getDriverState())
                .tripProgressState(state.getTripProgressState())
                .latitude(state.getLatitude()).longitude(state.getLongitude())
                .currentSpeed(state.getCurrentSpeed())
                .lastUpdatedAt(state.getLastUpdatedAt())
                .build();
    }
}
