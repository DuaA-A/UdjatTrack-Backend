package com.udjattrack.service.impl;

import com.udjattrack.dto.request.TelemetryBatchRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.TelemetryRecordResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.dto.websocket.TripStateUpdateMessage;
import com.udjattrack.entity.*;
import com.udjattrack.entity.timeseries.TelemetryRecord;
import com.udjattrack.entity.enums.DriverState;
import com.udjattrack.entity.enums.TripProgressState;
import com.udjattrack.entity.enums.TripStatus;
import com.udjattrack.entity.enums.SeverityLevel;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.*;
import com.udjattrack.repository.timeseries.TelemetryRecordRepository;
import com.udjattrack.service.AlertService;
import com.udjattrack.service.TelemetryService;
import com.udjattrack.websocket.WebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TelemetryServiceImpl implements TelemetryService {

    private final TelemetryRecordRepository telemetryRecordRepository;
    private final TripRepository tripRepository;
    private final TripStateRepository tripStateRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final AlertService alertService;
    private final WebSocketPublisher webSocketPublisher;
    private final EventRecordRepository eventRecordRepository;
    private final TripLogRepository tripLogRepository;

    @Override
    public TelemetryRecordResponse ingestTelemetry(TelemetryRequest request) {
        // Verify trip exists in relational DB
        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", request.tripId()));

        if (trip.getStatus() == com.udjattrack.entity.enums.TripStatus.PLANNED) {
            throw new com.udjattrack.exception.BusinessException("Cannot ingest telemetry for a trip that has not started yet.");
        }

        LocalDateTime telemetryTime = request.timeStamp() != null ? request.timeStamp().toLocalDateTime() : LocalDateTime.now();

        if (trip.getTripLog() != null) {
            LocalDateTime start = trip.getTripLog().getActualStartTime();
            LocalDateTime end = trip.getTripLog().getActualEndTime();

            if (start != null && telemetryTime.isBefore(start)) {
                throw new com.udjattrack.exception.BusinessException("Telemetry timestamp cannot be before the trip's actual start time.");
            }
            if (end != null && telemetryTime.isAfter(end)) {
                throw new com.udjattrack.exception.BusinessException("Telemetry timestamp cannot be after the trip's actual end time.");
            }
        }

        // We now save telemetry regardless of trip status to maintain history.
        // If the trip is ON_BREAK, we still record the telemetry and notify the dashboard.
        if (trip.getStatus() == com.udjattrack.entity.enums.TripStatus.ON_BREAK) {
            log.info("Trip {} is ON_BREAK. Telemetry is being recorded for history.", trip.getTripId());
        }

        String locationStr = null;
        if (request.location() != null) {
            locationStr = request.location().lat() + "," + request.location().lng();
        }

        // Save to time-series DB
        TelemetryRecord record = TelemetryRecord.builder()
                .tripId(request.tripId())
                .speed(request.speed())
                .location(locationStr)
                .driverState(request.driverState())
                .details(request.payload())
                .timestamp(request.timeStamp() != null ? request.timeStamp().toLocalDateTime() : LocalDateTime.now())
                .build();
        TelemetryRecord saved = telemetryRecordRepository.save(record);

        // Update live trip state in relational DB
        TripState state = tripStateRepository.findByTripTripId(request.tripId()).orElse(null);
        if (state != null) {
            if (request.driverState() != null) state.setDriverState(request.driverState());
            if (request.location() != null) {
                state.setLatitude(String.valueOf(request.location().lat()));
                state.setLongitude(String.valueOf(request.location().lng()));
            }
            state.setCurrentSpeed(request.speed());

            // Handle Trip State transition from payload if present
            if (request.payload() != null && request.payload().containsKey("tripState")) {
                String newStateStr = String.valueOf(request.payload().get("tripState"));
                try {
                    TripStatus newStatus = TripStatus.valueOf(newStateStr.toUpperCase());
                    if (trip.getStatus() != newStatus) {
                        log.info("Trip {} status changing from {} to {} via telemetry", trip.getTripId(), trip.getStatus(), newStatus);
                        
                        TripStatus oldStatus = trip.getStatus();
                        trip.setStatus(newStatus);
                        
                        // Map TripStatus to TripProgressState
                        TripProgressState progressState = switch (newStatus) {
                            case ONGOING -> TripProgressState.STARTED;
                            case ON_BREAK -> TripProgressState.PAUSED;
                            case FINISHED -> TripProgressState.COMPLETED;
                            default -> state.getTripProgressState();
                        };
                        state.setTripProgressState(progressState);

                        // Handle TripLog start/end
                        if (newStatus == TripStatus.ONGOING && oldStatus == TripStatus.PLANNED) {
                            if (trip.getTripLog() == null) {
                                TripLog tripLog = TripLog.builder()
                                        .trip(trip)
                                        .actualStartTime(telemetryTime)
                                        .build();
                                tripLogRepository.save(tripLog);
                                trip.setTripLog(tripLog);
                            }
                            trip.getDriver().setIdle(false);
                            trip.getVehicle().setIdle(false);
                        } else if (newStatus == TripStatus.FINISHED) {
                            tripLogRepository.findByTripTripId(trip.getTripId()).ifPresent(tl -> {
                                tl.setActualEndTime(telemetryTime);
                                if (tl.getActualStartTime() != null) {
                                    tl.setTotalDuration((double) java.time.Duration.between(tl.getActualStartTime(), tl.getActualEndTime()).toMinutes());
                                }
                                tripLogRepository.save(tl);
                            });
                            trip.getDriver().setIdle(true);
                            trip.getVehicle().setIdle(true);
                        }

                        // Save updated trip
                        tripRepository.save(trip);
                        driverRepository.save(trip.getDriver());
                        vehicleRepository.save(trip.getVehicle());

                        // Record in Timeline (EventRecord)
                        EventRecord statusEvent = EventRecord.builder()
                                .trip(trip)
                                .eventType("STATUS_CHANGE")
                                .severity(SeverityLevel.LOW)
                                .payload(java.util.Map.of("oldStatus", oldStatus, "newStatus", newStatus))
                                .timestamp(telemetryTime)
                                .build();
                        eventRecordRepository.save(statusEvent);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid tripState received in telemetry for trip {}: {}", trip.getTripId(), newStateStr);
                }
            }

            tripStateRepository.save(state);
            
            // Push update to fleet manager over WebSocket
            UUID managerId = trip.getDriver().getFleetManager().getUserId();
            webSocketPublisher.publishLiveTracking(managerId, TripStateUpdateMessage.builder()
                    .tripStateId(state.getStateId())
                    .tripId(trip.getTripId())
                    .driverState(state.getDriverState())
                    .tripProgressState(state.getTripProgressState())
                    .progressPct(calculateProgressPct(trip))
                    .location(java.util.Map.of("lat", state.getLatitude(), "long", state.getLongitude()))
                    .lastUpdatedAt(telemetryTime)
                    .build());
        }

        return toResponse(saved);
    }

    @Override
    @Async("telemetryExecutor")
    public void processTelemetryBatch(TelemetryBatchRequest batchRequest) {
        log.info("Processing telemetry batch: {} records", batchRequest.records().size());
        batchRequest.records().forEach(record -> {
            try {
                ingestTelemetry(record);
            } catch (Exception e) {
                log.error("Failed to process telemetry record for trip {}: {}",
                        record.tripId(), e.getMessage());
            }
        });
    }

    @Override
    public TripStateResponse updateTripState(UUID tripId, String driverState, String progressState) {
        TripState state = tripStateRepository.findByTripTripId(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("TripState", "tripId", tripId));
        if (driverState != null) {
            state.setDriverState(DriverState.valueOf(driverState));
        }
        return toStateResponse(tripStateRepository.save(state));
    }

    @Override
    public void updateDriverStatus(UUID driverId, String newState) {
        driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));
        log.info("Driver {} status updated to: {}", driverId, newState);
        
        // Push update to specific driver's private channel
        webSocketPublisher.publishDriverStatus(driverId, java.util.Map.of("status", newState, "timestamp", LocalDateTime.now()));
    }

    @Override
    public void updateVehicleState(UUID vehicleId, boolean isWorking) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));
        vehicle.setWorking(isWorking);
        vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripStateResponse> getLiveFleetStatus(UUID fleetManagerId) {
        return tripRepository.findAllByFleetManager(fleetManagerId).stream()
                .map(trip -> tripStateRepository.findByTripTripId(trip.getTripId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(this::toStateResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TelemetryRecordResponse> getTelemetryByTrip(UUID tripId) {
        return telemetryRecordRepository.findAllByTripIdOrderByTimestampDesc(tripId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private TelemetryRecordResponse toResponse(TelemetryRecord r) {
        return TelemetryRecordResponse.builder()
                .id(r.getId()).tripId(r.getTripId())
                .speed(r.getSpeed()).location(r.getLocation())
                .driverState(r.getDriverState()).details(r.getDetails())
                .timestamp(r.getTimestamp())
                .build();
    }

    private TripStateResponse toStateResponse(TripState s) {
        return TripStateResponse.builder()
                .stateId(s.getStateId()).tripId(s.getTrip().getTripId())
                .driverState(s.getDriverState()).tripProgressState(s.getTripProgressState())
                .latitude(s.getLatitude()).longitude(s.getLongitude())
                .currentSpeed(s.getCurrentSpeed())
                .lastUpdatedAt(s.getLastUpdatedAt())
                .build();
    }

    private Integer calculateProgressPct(Trip trip) {
        if (trip.getStatus() == TripStatus.FINISHED) return 100;
        if (trip.getTripLog() == null || trip.getTripLog().getActualStartTime() == null) return 0;
        
        LocalDateTime start = trip.getTripLog().getActualStartTime();
        if (trip.getScheduledStartTime() != null && trip.getScheduledEndTime() != null) {
            long totalMinutes = java.time.Duration.between(trip.getScheduledStartTime(), trip.getScheduledEndTime()).toMinutes();
            if (totalMinutes <= 0) return 0;
            long elapsedMinutes = java.time.Duration.between(start, LocalDateTime.now()).toMinutes();
            return (int) Math.min(100, Math.max(0, (elapsedMinutes * 100) / totalMinutes));
        }
        return 0;
    }
}
