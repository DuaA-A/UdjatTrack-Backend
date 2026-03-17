package com.udjattrack.controller;

import com.udjattrack.dto.request.TelemetryBatchRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.TelemetryRecordResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * TelemetryController — receives IoT telemetry from driver devices,
 * updates live state, and enables fleet-wide monitoring.
 */
@RestController
@RequestMapping("/telemetry")
@RequiredArgsConstructor
@Tag(name = "Telemetry", description = "Ingest real-time telemetry, update states, monitor fleet")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping("/ingest")
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Ingest a single telemetry record")
    public ResponseEntity<ApiResponse<TelemetryRecordResponse>> ingestTelemetry(
            @Valid @RequestBody TelemetryRequest request) {
        TelemetryRecordResponse response = telemetryService.ingestTelemetry(request);
        return ResponseEntity.ok(ApiResponse.ok("Telemetry recorded", response));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Upload telemetry batch (async processing)")
    public ResponseEntity<ApiResponse<Void>> processBatch(
            @Valid @RequestBody TelemetryBatchRequest request) {
        telemetryService.processTelemetryBatch(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Batch accepted for processing")
                .build());
    }

    @PatchMapping("/trips/{tripId}/state")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Update trip progress and driver state")
    public ResponseEntity<ApiResponse<TripStateResponse>> updateTripState(
            @PathVariable UUID tripId,
            @RequestParam String driverState,
            @RequestParam String progressState) {
        return ResponseEntity.ok(ApiResponse.ok("State updated",
                telemetryService.updateTripState(tripId, driverState, progressState)));
    }

    @PatchMapping("/drivers/{driverId}/status")
    @PreAuthorize("hasAnyRole('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Update driver alertness status")
    public ResponseEntity<ApiResponse<Void>> updateDriverStatus(
            @PathVariable UUID driverId,
            @RequestParam String newState) {
        telemetryService.updateDriverStatus(driverId, newState);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Driver status updated").build());
    }

    @PatchMapping("/vehicles/{vehicleId}/state")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Update vehicle operational state")
    public ResponseEntity<ApiResponse<Void>> updateVehicleState(
            @PathVariable UUID vehicleId,
            @RequestParam boolean isWorking) {
        telemetryService.updateVehicleState(vehicleId, isWorking);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Vehicle state updated").build());
    }

    @GetMapping("/fleet/{managerId}/live")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get live status of entire fleet")
    public ResponseEntity<ApiResponse<List<TripStateResponse>>> getLiveFleetStatus(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                telemetryService.getLiveFleetStatus(managerId)));
    }

    @GetMapping("/trips/{tripId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get telemetry history for a trip")
    public ResponseEntity<ApiResponse<List<TelemetryRecordResponse>>> getTelemetryByTrip(
            @PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(
                telemetryService.getTelemetryByTrip(tripId)));
    }
}
