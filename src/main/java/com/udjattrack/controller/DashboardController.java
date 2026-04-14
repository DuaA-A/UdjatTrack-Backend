package com.udjattrack.controller;

import com.udjattrack.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Monitoring", description = "High-level fleet analytics and live operational status")
public class DashboardController {

    @GetMapping("/{fleetId}/alerts-summary")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Aggregated alert statistics for the fleet manager")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertsSummary(@PathVariable UUID fleetId) {
        // Mock data matching design doc
        var data = Map.of(
            "totalToday", 24,
            "critical", 3,
            "unacknowledged", 11,
            "byType", Map.of(
                "DROWSINESS", 12,
                "OBSTACLE_NEAR", 8,
                "CRASH", 1
            )
        );
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{fleetId}/fleet-status")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Active vehicles and trips KPIs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFleetStatus(@PathVariable UUID fleetId) {
        var data = Map.of(
            "activeVehicles", 10,
            "activeVehiclesChangePct", 5,
            "activeTrips", 4,
            "activeTripsChangePct", 8
        );
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/drivers-status")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Online/offline/idle/driving status of all drivers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriversStatus() {
        var data = Map.of(
            "totalDrivers", 45,
            "driving", 12,
            "idle", 28,
            "offline", 5
        );
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
