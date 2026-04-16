package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateTripRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.service.EmergencyService;
import com.udjattrack.service.EventService;
import com.udjattrack.service.TripService;
import com.udjattrack.util.SecurityUtils;
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

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Tag(name = "Trip Management", description = "Create, assign, track, and manage trip lifecycle")
public class TripController {

    private final TripService tripService;
    private final EmergencyService emergencyService;
    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Create a new planned trip")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody CreateTripRequest request) {
        TripResponse trip = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Trip created in PLANNED state", trip));
    }

    @PostMapping("/{tripId}/start")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    @Operation(summary = "Driver starts a PLANNED trip")
    public ResponseEntity<ApiResponse<TripResponse>> startTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip started successfully", tripService.startTrip(tripId)));
    }

    @PostMapping("/{tripId}/pause")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    @Operation(summary = "Driver begins a rest break (ONGOING → ON_BREAK)")
    public ResponseEntity<ApiResponse<TripResponse>> pauseTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip paused for break", tripService.stopTrip(tripId)));
    }

    @PostMapping("/{tripId}/resume")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    @Operation(summary = "Driver ends break and resumes trip (ON_BREAK → ONGOING)")
    public ResponseEntity<ApiResponse<TripResponse>> resumeTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip resumed successfully", tripService.resumeTrip(tripId)));
    }

    @PostMapping("/{tripId}/end")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER')")
    @Operation(summary = "Driver ends the trip (ONGOING → FINISHED)")
    public ResponseEntity<ApiResponse<TripResponse>> endTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip ended successfully. Summary generation triggered.", tripService.completeTrip(tripId)));
    }

    @PostMapping("/{tripId}/cancel")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Fleet Manager remotely cancels/terminates an active trip")
    public ResponseEntity<ApiResponse<TripResponse>> cancelTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip cancelled by management", tripService.cancelTrip(tripId)));
    }

    @GetMapping("/{tripId}/timeline")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get the trip event timeline for the Trip Log screen")
    public ResponseEntity<ApiResponse<List<com.udjattrack.entity.EventRecord>>> getTimeline(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Timeline retrieved", eventService.getEventsByTrip(tripId)));
    }

    @PostMapping("/{tripId}/issues/maintenance")
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Submit Maintenance issue from mobile app")
    public ResponseEntity<ApiResponse<com.udjattrack.dto.response.MaintenanceRequestResponse>> reportMaintenance(
            @PathVariable UUID tripId, 
            @RequestBody @Valid com.udjattrack.dto.request.CreateMaintenanceRequest payload) {
        return ResponseEntity.ok(ApiResponse.ok("Maintenance issue reported", 
                emergencyService.reportMaintenanceRequest(payload)));
    }

    @PostMapping("/{tripId}/issues/sos")
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Submit SOS emergency from mobile app")
    public ResponseEntity<ApiResponse<com.udjattrack.dto.response.SOSRequestResponse>> reportSOS(
            @PathVariable UUID tripId, 
            @RequestBody @Valid com.udjattrack.dto.request.CreateSOSRequest payload) {
        return ResponseEntity.ok(ApiResponse.ok("SOS alert triggered", 
                emergencyService.sendManualSOS(payload)));
    }

    @GetMapping("/{tripId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Retrieves full trip details")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripById(tripId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "List trips with filters (status, driverId, vehicleId, etc.)")
    public ResponseEntity<ApiResponse<List<TripResponse>>> listTrips(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID driverId) {
        
        if (driverId != null) {
            return ResponseEntity.ok(ApiResponse.ok(tripService.getTripsByDriver(driverId)));
        }
        
        // If no driverId, return all trips the current manager is allowed to see
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Trips retrieved", 
                tripService.getTripsByFleetManager(currentUserId)));
    }
}
