package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateTripRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.dto.response.TripTimelineResponse;
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
    @PreAuthorize("hasAuthority('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Create a new planned trip")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @Valid @RequestBody CreateTripRequest request) {
        TripResponse trip = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Trip created in PLANNED state", trip));
    }

    @PatchMapping("/{tripId}")
    @PreAuthorize("hasAuthority('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Update an existing PLANNED trip")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable UUID tripId,
            @RequestBody com.udjattrack.dto.request.UpdateTripRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Trip updated successfully", tripService.updateTrip(tripId, request)));
    }

    @PostMapping("/{tripId}/start")
    @PreAuthorize("hasAnyAuthority('ROLE_DRIVER')")
    @Operation(summary = "Driver starts a PLANNED trip")
    public ResponseEntity<ApiResponse<TripResponse>> startTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip started successfully", tripService.startTrip(tripId)));
    }

    @PostMapping("/{tripId}/Paused")
    @PreAuthorize("hasAnyAuthority('ROLE_DRIVER')")
    @Operation(summary = "Driver begins a rest break (ONGOING → ON_BREAK)")
    public ResponseEntity<ApiResponse<TripResponse>> pauseTrip(@PathVariable UUID tripId, @Valid @RequestBody(required = false) com.udjattrack.dto.request.LocationDTO location) {
        return ResponseEntity.ok(ApiResponse.ok("Trip paused for break", tripService.stopTrip(tripId, location)));
    }

    @PostMapping("/{tripId}/Resumed")
    @PreAuthorize("hasAnyAuthority('ROLE_DRIVER')")
    @Operation(summary = "Driver ends break and resumes trip (ON_BREAK → ONGOING)")
    public ResponseEntity<ApiResponse<TripResponse>> resumeTrip(@PathVariable UUID tripId, @Valid @RequestBody(required = false) com.udjattrack.dto.request.LocationDTO location) {
        return ResponseEntity.ok(ApiResponse.ok("Trip resumed successfully", tripService.resumeTrip(tripId, location)));
    }

    @PostMapping("/{tripId}/end")
    @PreAuthorize("hasAnyAuthority('ROLE_DRIVER')")
    @Operation(summary = "Driver ends the trip (ONGOING → FINISHED)")
    public ResponseEntity<ApiResponse<TripResponse>> endTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip ended successfully. Summary generation triggered.", tripService.completeTrip(tripId)));
    }

    @PostMapping("/{tripId}/cancel")
    @PreAuthorize("hasAuthority('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Fleet Manager remotely cancels/terminates an active trip")
    public ResponseEntity<ApiResponse<TripResponse>> cancelTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip cancelled by management", tripService.cancelTrip(tripId)));
    }

    @GetMapping("/{tripId}/timeline")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get the trip event timeline for the Trip Log screen")
    public ResponseEntity<ApiResponse<TripTimelineResponse>> getTimeline(
            @PathVariable UUID tripId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(ApiResponse.ok("Trip timeline retrieved successfully", tripService.getTripTimeline(tripId, from, to)));
    }

    @GetMapping("/{tripId}/progress")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get the real-time progress of a trip")
    public ResponseEntity<ApiResponse<TripStateResponse>> getTripProgress(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip progress retrieved", tripService.trackTripProgress(tripId)));
    }

    @PostMapping("/{tripId}/issues/maintenance")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    @Operation(summary = "Submit Maintenance issue from mobile app")
    public ResponseEntity<ApiResponse<com.udjattrack.dto.response.MaintenanceRequestResponse>> reportMaintenance(
            @PathVariable UUID tripId, 
            @RequestBody @Valid com.udjattrack.dto.request.CreateMaintenanceRequest payload) {
        
        com.udjattrack.dto.request.CreateMaintenanceRequest finalRequest = new com.udjattrack.dto.request.CreateMaintenanceRequest(
                tripId, 
                payload.maintenanceType(), 
                payload.payload()
        );
        
        return ResponseEntity.ok(ApiResponse.ok("Maintenance issue reported", 
                emergencyService.reportMaintenanceRequest(finalRequest)));
    }
    
    @PostMapping("/{tripId}/issues/sos")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    @Operation(summary = "Submit SOS emergency from mobile app")
    public ResponseEntity<ApiResponse<com.udjattrack.dto.response.SOSRequestResponse>> reportSOS(
            @PathVariable UUID tripId, 
            @RequestBody @Valid com.udjattrack.dto.request.CreateSOSRequest payload) {
    
        com.udjattrack.dto.request.CreateSOSRequest finalRequest = new com.udjattrack.dto.request.CreateSOSRequest(
                tripId, 
                payload.location(), 
                payload.autoTriggered(), 
                payload.payload()
        );
        
        return ResponseEntity.ok(ApiResponse.ok("SOS alert triggered", 
                emergencyService.sendManualSOS(finalRequest)));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAuthority('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Get all trip reports containing trip logs for the logged-in Fleet Manager")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getAllTripReports() {
        UUID managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("All trip reports retrieved successfully", tripService.getTripsByFleetManager(managerId)));
    }

    @GetMapping("/{tripId}/report")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get trip report containing trip log and other details of a single trip")
    public ResponseEntity<ApiResponse<TripResponse>> getTripReport(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok("Trip report retrieved successfully", tripService.getTripReport(tripId)));
    }

    @GetMapping("/{tripId}")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Retrieves full trip details")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripById(tripId)));
    }

    @GetMapping("/driver/dashboard")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    @Operation(summary = "Get driver dashboard data (current, today, previous, upcoming trips)")
    public ResponseEntity<ApiResponse<com.udjattrack.dto.response.DriverDashboardResponse>> getDriverDashboard() {
        UUID driverId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Driver dashboard retrieved", 
                tripService.getDriverDashboard(driverId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "List trips with filters (status, driverId, vehicleId, timeframe, etc.)")
    public ResponseEntity<ApiResponse<List<TripResponse>>> listTrips(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "driverId") UUID driverIdParam,
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isDriver = SecurityUtils.hasRole("ROLE_DRIVER");

        UUID targetDriverId = (isDriver && driverIdParam == null) ? currentUserId : driverIdParam;
        
        if (targetDriverId != null) {
            return ResponseEntity.ok(ApiResponse.ok("Trips retrieved", tripService.getTripsByDriver(targetDriverId, status)));
        }
        
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok("Trips retrieved", 
                    tripService.getTripsByFleetManagerWithFilters(currentUserId, status, vehicleId, dateFrom, dateTo)));
        }
        
        return ResponseEntity.ok(ApiResponse.ok("Trips retrieved", 
                tripService.getTripsByFleetManager(currentUserId)));
    }
}
