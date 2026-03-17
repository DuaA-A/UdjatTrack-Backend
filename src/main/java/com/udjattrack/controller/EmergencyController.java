package com.udjattrack.controller;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.*;
import com.udjattrack.service.EmergencyService;
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
 * EmergencyController — handles SOS requests, incidents, and maintenance reports.
 * Drivers trigger; Fleet managers and super managers respond.
 */
@RestController
@RequestMapping("/emergency")
@RequiredArgsConstructor
@Tag(name = "Emergency", description = "SOS requests, incident reporting, and maintenance issues")
public class EmergencyController {

    private final EmergencyService emergencyService;

    @PostMapping("/sos/manual")
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Send manual SOS from driver")
    public ResponseEntity<ApiResponse<SOSRequestResponse>> sendManualSOS(
            @Valid @RequestBody CreateSOSRequest request) {
        SOSRequestResponse response = emergencyService.sendManualSOS(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("SOS sent", response));
    }

    @PostMapping("/sos/auto")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Auto-triggered SOS (from system/device detection)")
    public ResponseEntity<ApiResponse<SOSRequestResponse>> sendAutoSOS(
            @Valid @RequestBody CreateSOSRequest request) {
        SOSRequestResponse response = emergencyService.sendAutoSOS(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Auto SOS dispatched", response));
    }

    @PostMapping("/sos/{sosId}/notify-emergency-services")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Notify external emergency services for an SOS")
    public ResponseEntity<ApiResponse<Void>> notifyEmergencyServices(@PathVariable UUID sosId) {
        emergencyService.notifyEmergencyService(sosId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Emergency services notified").build());
    }

    @PostMapping("/maintenance")
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Report a maintenance issue")
    public ResponseEntity<ApiResponse<MaintenanceRequestResponse>> reportMaintenance(
            @Valid @RequestBody CreateMaintenanceRequest request) {
        MaintenanceRequestResponse response = emergencyService.reportMaintenanceRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Maintenance request submitted", response));
    }

    @PostMapping("/incidents")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Create an incident report")
    public ResponseEntity<ApiResponse<IncidentResponse>> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {
        IncidentResponse response = emergencyService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Incident reported", response));
    }

    @GetMapping("/sos/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get SOS requests for a fleet manager's fleet")
    public ResponseEntity<ApiResponse<List<SOSRequestResponse>>> getSOSRequests(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                emergencyService.getSOSRequestsByFleetManager(managerId)));
    }

    @GetMapping("/maintenance/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get maintenance requests for a fleet manager's fleet")
    public ResponseEntity<ApiResponse<List<MaintenanceRequestResponse>>> getMaintenanceRequests(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                emergencyService.getMaintenanceRequestsByFleetManager(managerId)));
    }

    @GetMapping("/incidents/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get incident reports for a fleet manager's fleet")
    public ResponseEntity<ApiResponse<List<IncidentResponse>>> getIncidents(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                emergencyService.getIncidentsByFleetManager(managerId)));
    }
}
