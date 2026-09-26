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
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Monitoring", description = "High-level fleet analytics and live operational status")
public class DashboardController {
 
    private final AlertService alertService;
    private final TripService tripService;
    private final FleetManagementService fleetManagementService;

    @GetMapping("/{fleetId}/alerts-summary")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Aggregated alert statistics for the fleet manager")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlertsSummary(@PathVariable UUID fleetId) {
        List<AlertResponse> allAlerts = alertService.getAllAlertsByFleetManager(fleetId);

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        List<AlertResponse> todays = allAlerts.stream()
            .filter(a -> a.getTimestamp() != null &&
                (a.getTimestamp().isEqual(startOfToday) || (a.getTimestamp().isAfter(startOfToday) && a.getTimestamp().isBefore(startOfTomorrow)) || a.getTimestamp().isEqual(startOfTomorrow)))
            .toList();

        long criticalCount = todays.stream()
            .filter(a -> a.getSeverity() != null && a.getSeverity().name().equalsIgnoreCase("CRITICAL"))
            .count();

        long unacknowledgedCount = todays.stream()
            .filter(a -> !Boolean.TRUE.equals(a.getAcknowledged()))
            .count();

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("totalToday", todays.size());
        data.put("critical", criticalCount);
        data.put("unacknowledged", unacknowledgedCount);
        data.put("byType", todays.stream()
            .collect(java.util.stream.Collectors.groupingBy(a -> a.getAlertType().name(), java.util.stream.Collectors.counting())));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{fleetId}/fleet-status")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Active vehicles and trips KPIs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFleetStatus(@PathVariable UUID fleetId) {
        List<TripResponse> activeTrips = tripService.getTripsByFleetManager(fleetId).stream()
                .filter(t -> !"FINISHED".equalsIgnoreCase(t.getStatus().name()) && !"CANCELLED".equalsIgnoreCase(t.getStatus().name()))
                .toList();
                
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("activeVehicles", activeTrips.stream().map(TripResponse::getVehicleId).distinct().count());
        data.put("activeVehiclesChangePct", 0);
        data.put("activeTrips", activeTrips.size());
        data.put("activeTripsChangePct", 0);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/drivers-status")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get driver status counts from active trips")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriversStatus() {
        UUID managerId = SecurityUtils.getCurrentUserId();
        
        List<TripResponse> activeTrips = tripService.getTripsByFleetManagerWithFilters(
                managerId, "active", null, null, null);
        
        long alertCount = 0;
        long drowsyCount = 0;
        long highRiskCount = 0;

        for (TripResponse trip : activeTrips) {
            if (trip.getTripState() != null && trip.getTripState().getDriverState() != null) {
                switch (trip.getTripState().getDriverState().name()) {
                    case "ALERT":
                        alertCount++;
                        break;
                    case "DROWSY":
                        drowsyCount++;
                        break;
                    case "HIGH_RISK":
                        highRiskCount++;
                        break;
                }
            }
        }

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("alert", alertCount);
        data.put("drowsy", drowsyCount);
        data.put("highRisk", highRiskCount);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/active-trips")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get list of active trips (Ongoing or On Break)")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getActiveTrips(
            @RequestParam(required = false) UUID tripId) {
        UUID managerId = SecurityUtils.getCurrentUserId();
        List<TripResponse> activeTrips = tripService.getTripsByFleetManagerWithFilters(
                managerId, "active", null, null, null);
        
        if (tripId != null) {
            activeTrips = activeTrips.stream()
                    .filter(t -> t.getTripId().equals(tripId))
                    .toList();
        }
        
        return ResponseEntity.ok(ApiResponse.ok(activeTrips));
    }

    @GetMapping("/trip-summary-table")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get trip summary table data")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripSummaryTable() {
        UUID managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripsByFleetManager(managerId)));
    }
}
