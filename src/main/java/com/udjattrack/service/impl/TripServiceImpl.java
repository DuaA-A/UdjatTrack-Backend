package com.udjattrack.service.impl;

import com.udjattrack.dto.request.CreateTripRequest;
import com.udjattrack.dto.response.TripLogResponse;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.entity.*;
import com.udjattrack.entity.enums.TripProgressState;
import com.udjattrack.entity.enums.TripStatus;
import com.udjattrack.exception.BusinessException;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.*;
import com.udjattrack.service.NotificationService;
import com.udjattrack.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
        updateTripProgressState(trip, TripProgressState.STARTED);

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
    public TripResponse stopTrip(UUID tripId) {
        Trip trip = findTripOrThrow(tripId);
        if (trip.getStatus() != TripStatus.ONGOING) {
            throw new BusinessException("Only ONGOING trips can be stopped (paused)");
        }
        updateTripProgressState(trip, TripProgressState.PAUSED);
        return toResponse(trip);
    }

    @Override
    public TripResponse resumeTrip(UUID tripId) {
        Trip trip = findTripOrThrow(tripId);
        if (trip.getStatus() != TripStatus.ONGOING) {
            throw new BusinessException("Only ONGOING trips can be resumed");
        }
        updateTripProgressState(trip, TripProgressState.RESUMED);
        return toResponse(trip);
    }

    @Override
    public TripResponse completeTrip(UUID tripId) {
        Trip trip = findTripOrThrow(tripId);
        trip.setStatus(TripStatus.FINISHED);
        updateTripProgressState(trip, TripProgressState.COMPLETED);

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
        // Clear progress state if cancelled? Or leave it as is.
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

        List<TripStatus> targetStatuses = switch (status.toLowerCase()) {
            case "active" -> List.of(TripStatus.ONGOING);
            case "past" -> List.of(TripStatus.FINISHED, TripStatus.CANCELLED);
            case "upcoming" -> List.of(TripStatus.PLANNED);
            default -> throw new BusinessException("Invalid trip status filter. Use: active, past, upcoming");
        };

        return tripRepository.findAllByDriverUserIdAndStatusInOrderByCreatedAtDesc(driverId, targetStatuses)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByFleetManager(UUID managerId) {
        return tripRepository.findAllByFleetManager(managerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
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

    private void updateTripProgressState(Trip trip, TripProgressState state) {
        tripStateRepository.findByTripTripId(trip.getTripId()).ifPresent(ts -> {
            ts.setTripProgressState(state);
            tripStateRepository.save(ts);
        });
    }

    private TripResponse toResponse(Trip trip) {
        TripLogResponse logResponse = null;
        LocalDateTime actualStartTime = null;
        if (trip.getTripLog() != null) {
            TripLog tl = trip.getTripLog();
            actualStartTime = tl.getActualStartTime();
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
                .totalDistanceKm(0.0) // Placeholder
                .build();
    }

    private TripStateResponse toStateResponse(TripState state) {
        return TripStateResponse.builder()
                .stateId(state.getStateId()).tripId(state.getTrip().getTripId())
                .driverState(state.getDriverState())
                .tripProgressState(state.getTripProgressState())
                .latitude(state.getLatitude()).longitude(state.getLongitude())
                .lastUpdatedAt(state.getLastUpdatedAt())
                .build();
    }
}
