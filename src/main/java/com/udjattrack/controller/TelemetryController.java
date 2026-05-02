package com.udjattrack.controller;

import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.TelemetryRecordResponse;
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

@RestController
@RequestMapping("/trips/{tripId}/telemetry")
@RequiredArgsConstructor
@Tag(name = "Telemetry", description = "Ingest real-time telemetry from mobile and IoT devices")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Send periodic heartbeat snapshot")
    public ResponseEntity<ApiResponse<Void>> ingestTelemetry(
            @PathVariable UUID tripId,
            @Valid @RequestBody TelemetryRequest request) {
        // We should ensure the request.tripId matches PathVariable tripId
        telemetryService.ingestTelemetry(request);
        return ResponseEntity.accepted()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Telemetry recorded")
                        .timestamp(request.timeStamp())
                        .build());
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Upload batched telemetry records (used when recovering from offline status)")
    public ResponseEntity<ApiResponse<String>> ingestTelemetryBatch(
            @PathVariable UUID tripId,
            @Valid @RequestBody com.udjattrack.dto.request.TelemetryBatchRequest request) {
        telemetryService.processTelemetryBatch(request);
        return ResponseEntity.accepted()
                .body(ApiResponse.<String>builder()
                        .success(true)
                        .message("Batch telemetry queued for processing")
                        .build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Retrieve telemetry history for a trip with time filters")
    public ResponseEntity<ApiResponse<List<TelemetryRecordResponse>>> getTelemetryByTrip(
            @PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(
                telemetryService.getTelemetryByTrip(tripId)));
    }
}
