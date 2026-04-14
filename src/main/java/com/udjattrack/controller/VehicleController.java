package com.udjattrack.controller;

import com.udjattrack.dto.request.AddVehicleRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.VehicleResponse;
import com.udjattrack.service.FleetManagementService;
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
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle Management", description = "Register and manage fleet vehicles")
public class VehicleController {

    private final FleetManagementService fleetManagementService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Register a vehicle in the fleet")
    public ResponseEntity<ApiResponse<VehicleResponse>> addVehicle(
            @Valid @RequestBody AddVehicleRequest request) {
        UUID managerId = SecurityUtils.getCurrentUserId();
        VehicleResponse response = fleetManagementService.addVehicle(managerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Vehicle registered", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER', 'ROLE_DRIVER')")
    @Operation(summary = "Get vehicle details")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(fleetManagementService.getVehicleById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "List all vehicles in the fleet")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAllVehicles() {
        UUID managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(fleetManagementService.getVehiclesByManager(managerId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Remove a vehicle from the fleet")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable UUID id) {
        fleetManagementService.deleteVehicle(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Vehicle deleted").build());
    }

    @GetMapping("/{vehicleId}/maintenance-logs")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER')")
    @Operation(summary = "Populates the Vehicle Maintenance CheckList widget")
    public ResponseEntity<ApiResponse<List<com.udjattrack.dto.response.MaintenanceLogResponse>>> getMaintenanceLogs(@PathVariable UUID vehicleId) {
        // Returning a mock list as per design doc example for now
        var logs = List.of(
            com.udjattrack.dto.response.MaintenanceLogResponse.builder()
                .date("2026-04-10")
                .mileage("19,210")
                .details("Engine Check")
                .status("PASS")
                .build()
        );
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
