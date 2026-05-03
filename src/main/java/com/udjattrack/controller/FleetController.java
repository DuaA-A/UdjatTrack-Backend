package com.udjattrack.controller;

import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.service.FleetService;
import com.udjattrack.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fleets")
@RequiredArgsConstructor
@Tag(name = "Fleet Operations", description = "High-level fleet monitoring and reporting")
public class FleetController {

    private final FleetService fleetService;

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get real-time status of the entire fleet (active trips)")
    public ResponseEntity<ApiResponse<List<TripStateResponse>>> getFleetStatus() {
        UUID managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Fleet status retrieved", 
                fleetService.getFleetStatus(managerId)));
    }

    @GetMapping("/{fleetId}/reports")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get aggregated fleet reports/trips")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getFleetReports(@PathVariable UUID fleetId) {
        // fleetId is usually the managerId or a specific group ID.
        // For now, we assume fleetId = managerId.
        return ResponseEntity.ok(ApiResponse.ok("Fleet reports retrieved", 
                fleetService.getFleetReports(fleetId)));
    }
}
