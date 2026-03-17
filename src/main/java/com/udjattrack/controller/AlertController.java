package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.AlertResponse;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.service.AlertService;
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
 * AlertController — manages alert creation, acknowledgment, and notification dispatch.
 */
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Management", description = "Create, acknowledge, and query trip alerts")
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Create an alert manually")
    public ResponseEntity<ApiResponse<AlertResponse>> createAlert(
            @Valid @RequestBody CreateAlertRequest request) {
        AlertResponse alert = alertService.createAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Alert created", alert));
    }

    @PatchMapping("/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Acknowledge an alert")
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledgeAlert(@PathVariable UUID alertId) {
        return ResponseEntity.ok(ApiResponse.ok("Alert acknowledged",
                alertService.acknowledgeAlert(alertId)));
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Evaluate a telemetry event for alert conditions")
    public ResponseEntity<ApiResponse<Void>> evaluateEvent(
            @Valid @RequestBody TelemetryRequest request) {
        alertService.evaluateEvent(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Event evaluated").build());
    }

    @GetMapping("/unacknowledged/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get unacknowledged alerts for a fleet manager")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getUnacknowledged(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getUnacknowledgedAlerts(managerId)));
    }

    @GetMapping("/trip-log/{tripLogId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get all alerts for a specific trip log")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlertsByTripLog(
            @PathVariable UUID tripLogId) {
        return ResponseEntity.ok(ApiResponse.ok(alertService.getAlertsByTripLog(tripLogId)));
    }

    @PostMapping("/{alertId}/notify-manager")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Notify fleet manager of a specific alert")
    public ResponseEntity<ApiResponse<Void>> notifyManager(@PathVariable UUID alertId) {
        alertService.notifyFleetManager(alertId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Fleet manager notified").build());
    }
}
