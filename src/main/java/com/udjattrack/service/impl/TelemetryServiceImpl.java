package com.udjattrack.service.impl;

import com.udjattrack.dto.request.TelemetryBatchRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.TelemetryRecordResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.dto.websocket.TripStateUpdateMessage;
import com.udjattrack.entity.*;
import com.udjattrack.entity.timeseries.TelemetryRecord;
import com.udjattrack.entity.enums.DriverState;
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
                .details(request.details())
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
            tripStateRepository.save(state);
            
            // Push update to fleet manager over WebSocket
            UUID managerId = trip.getDriver().getFleetManager().getUserId();
            webSocketPublisher.publishLiveTracking(managerId, TripStateUpdateMessage.builder()
                    .tripId(trip.getTripId())
                    .driverState(state.getDriverState())
                    .tripProgressState(state.getTripProgressState())
                    .latitude(state.getLatitude())
                    .longitude(state.getLongitude())
                    .currentSpeed(request.speed())
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        // (Evaluation moved to EventService)

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
                .lastUpdatedAt(s.getLastUpdatedAt())
                .build();
    }
}
