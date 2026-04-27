package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.response.AlertResponse;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.service.AlertService;
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
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Management", description = "Query and manage safety alerts for drivers and vehicles")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER', 'ROLE_DRIVER')")
    @Operation(summary = "List alerts with filters")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID tripId,
            @RequestParam(required = false) String severity) {
        
        if ("UNACKNOWLEDGED".equalsIgnoreCase(status)) {
            UUID managerId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.ok(ApiResponse.ok(alertService.getUnacknowledgedAlerts(managerId)));
        }
        
        return ResponseEntity.ok(ApiResponse.ok("Query results", List.of()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get alert details")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlert(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getAlertById(id)));
    }

    @PostMapping("/{alertId}/ack")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_DRIVER')")
    @Operation(summary = "Acknowledge an alert")
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledgeAlert(@PathVariable UUID alertId) {
        return ResponseEntity.ok(ApiResponse.ok("Alert acknowledged",
                alertService.acknowledgeAlert(alertId)));
    }

    @PostMapping("/{alertId}/read")
    @PreAuthorize("hasAuthority('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Fleet manager marks alert notification as seen/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID alertId) {
        // Stub for now
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Alert marked as read")
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Create an alert manually (Administrator only)")
    public ResponseEntity<ApiResponse<AlertResponse>> createAlert(
            @Valid @RequestBody CreateAlertRequest request) {
        AlertResponse alert = alertService.createAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Alert created", alert));
    }
}
