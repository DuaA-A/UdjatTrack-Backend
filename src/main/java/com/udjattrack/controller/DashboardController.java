package com.udjattrack.controller;

import com.udjattrack.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.udjattrack.service.AlertService;
import com.udjattrack.service.FleetManagementService;
import com.udjattrack.service.TripService;
import com.udjattrack.util.SecurityUtils;
import com.udjattrack.dto.response.AlertResponse;
import com.udjattrack.dto.response.DriverResponse;
import com.udjattrack.dto.response.TripResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Monitoring", description = "High-level fleet analytics and live operational status")
public class DashboardController {
 
    private final AlertService alertService;
    private final TripService tripService;
    private final FleetManagementService fleetManagementService;

    @GetMapping("/{fleetId}/alerts-summary")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Aggregated alert statistics for the fleet manager")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertsSummary(@PathVariable UUID fleetId) {
        List<AlertResponse> unacknowledged = alertService.getUnacknowledgedAlerts(fleetId);
        
        long criticalCount = unacknowledged.stream()
                .filter(a -> "CRITICAL".equalsIgnoreCase(a.getSeverity().name()))
                .count();

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("totalToday", unacknowledged.size());
        data.put("critical", criticalCount);
        data.put("unacknowledged", unacknowledged.size());
        data.put("byType", unacknowledged.stream()
            .collect(java.util.stream.Collectors.groupingBy(a -> a.getAlertType().name(), java.util.stream.Collectors.counting())));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{fleetId}/fleet-status")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Active vehicles and trips KPIs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFleetStatus(@PathVariable UUID fleetId) {
        List<TripResponse> activeTrips = tripService.getTripsByFleetManager(fleetId).stream()
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getTripState().name()) && !"CANCELLED".equalsIgnoreCase(t.getTripState().name()))
                .toList();
                
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("activeVehicles", activeTrips.stream().map(TripResponse::getVehicleId).distinct().count());
        data.put("activeVehiclesChangePct", 0);
        data.put("activeTrips", activeTrips.size());
        data.put("activeTripsChangePct", 0);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/drivers-status")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Online/offline/idle/driving status of all drivers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriversStatus() {
        UUID managerId = SecurityUtils.getCurrentUserId();
        List<DriverResponse> drivers = fleetManagementService.getDriversByManager(managerId);
        
        long idleCount = drivers.stream().filter(DriverResponse::getIdle).count();
        long drivingCount = drivers.size() - idleCount; // Simplified

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("totalDrivers", drivers.size());
        data.put("driving", drivingCount);
        data.put("idle", idleCount);
        data.put("offline", 0);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
