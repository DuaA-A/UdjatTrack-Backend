package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateTripRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * TripController — manages the full lifecycle of a trip.
 * FleetManagers create/assign trips; Drivers track/update progress.
 */
@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Tag(name = "Trip Management", description = "Create, assign, track, and manage trip lifecycle")
public class TripController {

    private final TripService tripService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Create a new trip")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody CreateTripRequest request) {
        TripResponse trip = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Trip created", trip));
    }

    @PatchMapping("/{tripId}/assign-driver/{driverId}")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Assign a driver to a trip")
    public ResponseEntity<ApiResponse<TripResponse>> assignDriver(
            @PathVariable UUID tripId, @PathVariable UUID driverId) {
        return ResponseEntity.ok(ApiResponse.ok("Driver assigned",
                tripService.assignDriver(tripId, driverId)));
    }

    @PatchMapping("/{tripId}/assign-vehicle/{vehicleId}")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Assign a vehicle to a trip")
    public ResponseEntity<ApiResponse<TripResponse>> assignVehicle(
            @PathVariable UUID tripId, @PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ApiResponse.ok("Vehicle assigned",
                tripService.assignVehicle(tripId, vehicleId)));
    }

    @PatchMapping("/{tripId}/start")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Start a trip")
    public ResponseEntity<ApiResponse<TripResponse>> startTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip started", tripService.startTrip(tripId)));
    }

    @PatchMapping("/{tripId}/stop")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Pause / stop a trip")
    public ResponseEntity<ApiResponse<TripResponse>> stopTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip paused", tripService.stopTrip(tripId)));
    }

    @PatchMapping("/{tripId}/resume")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Resume a paused trip")
    public ResponseEntity<ApiResponse<TripResponse>> resumeTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip resumed", tripService.resumeTrip(tripId)));
    }

    @PatchMapping("/{tripId}/complete")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Complete a trip")
    public ResponseEntity<ApiResponse<TripResponse>> completeTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip completed", tripService.completeTrip(tripId)));
    }

    @PatchMapping("/{tripId}/cancel")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Cancel a trip")
    public ResponseEntity<ApiResponse<TripResponse>> cancelTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip cancelled", tripService.cancelTrip(tripId)));
    }

    @GetMapping("/{tripId}/progress")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get live trip progress state")
    public ResponseEntity<ApiResponse<TripStateResponse>> trackProgress(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.trackTripProgress(tripId)));
    }

    @GetMapping("/{tripId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get trip by ID")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripById(tripId)));
    }

    @GetMapping("/{tripId}/report")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get full trip report (with log, alerts, incidents)")
    public ResponseEntity<ApiResponse<TripResponse>> getTripReport(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripReport(tripId)));
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "List all trips for a driver")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripsByDriver(
            @PathVariable UUID driverId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripsByDriver(driverId)));
    }

    @GetMapping("/fleet-manager/{managerId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "List all trips for a fleet manager")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripsByManager(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripsByFleetManager(managerId)));
    }
}
