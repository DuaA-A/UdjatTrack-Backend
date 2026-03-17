package com.udjattrack.controller;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.*;
import com.udjattrack.service.FleetManagementService;
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
 * FleetManagementController — manages fleet managers, drivers, vehicles and dependents.
 *
 * Role mapping:
 *  - SuperManager: verify/delete/list fleet managers
 *  - FleetManager: CRUD drivers, vehicles, dependents
 */
@RestController
@RequestMapping("/fleet")
@RequiredArgsConstructor
@Tag(name = "Fleet Management", description = "Manage fleet managers, drivers, vehicles, and dependents")
public class FleetManagementController {

    private final FleetManagementService fleetManagementService;

    // ===== Fleet Manager (SuperManager only) =====

    @PostMapping("/managers")
    @PreAuthorize("hasRole('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Register fleet manager")
    public ResponseEntity<ApiResponse<FleetManagerResponse>> registerFleetManager(
            @Valid @RequestBody CreateFleetManagerRequest request) {
        FleetManagerResponse response = fleetManagementService.registerFleetManager(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Fleet manager registered", response));
    }

    @PatchMapping("/managers/{managerId}/verify")
    @PreAuthorize("hasRole('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Verify a fleet manager")
    public ResponseEntity<ApiResponse<FleetManagerResponse>> verifyFleetManager(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok("Manager verified",
                fleetManagementService.verifyFleetManager(managerId)));
    }

    @PutMapping("/managers/{managerId}")
    @PreAuthorize("hasRole('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Update a fleet manager")
    public ResponseEntity<ApiResponse<FleetManagerResponse>> updateFleetManager(
            @PathVariable UUID managerId,
            @Valid @RequestBody UpdateFleetManagerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Manager updated",
                fleetManagementService.updateFleetManager(managerId, request)));
    }

    @DeleteMapping("/managers/{managerId}")
    @PreAuthorize("hasRole('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Soft-delete a fleet manager")
    public ResponseEntity<ApiResponse<Void>> deleteFleetManager(@PathVariable UUID managerId) {
        fleetManagementService.deleteFleetManager(managerId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Fleet manager deleted").build());
    }

    @GetMapping("/managers")
    @PreAuthorize("hasRole('ROLE_SUPER_MANAGER')")
    @Operation(summary = "List all fleet managers")
    public ResponseEntity<ApiResponse<List<FleetManagerResponse>>> getAllManagers() {
        return ResponseEntity.ok(ApiResponse.ok(fleetManagementService.getAllFleetManagers()));
    }

    @GetMapping("/managers/{managerId}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_MANAGER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Get fleet manager by ID")
    public ResponseEntity<ApiResponse<FleetManagerResponse>> getManager(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                fleetManagementService.getFleetManagerById(managerId)));
    }

    // ===== Drivers (FleetManager) =====

    @PostMapping("/managers/{managerId}/drivers")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Create a driver under fleet")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(
            @PathVariable UUID managerId,
            @Valid @RequestBody CreateDriverRequest request) {
        DriverResponse response = fleetManagementService.createDriver(managerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Driver created", response));
    }

    @PutMapping("/drivers/{driverId}")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Update driver info")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(
            @PathVariable UUID driverId,
            @Valid @RequestBody UpdateDriverRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Driver updated",
                fleetManagementService.updateDriver(driverId, request)));
    }

    @DeleteMapping("/drivers/{driverId}")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Soft-delete a driver")
    public ResponseEntity<ApiResponse<Void>> deleteDriver(@PathVariable UUID driverId) {
        fleetManagementService.deleteDriver(driverId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Driver deleted").build());
    }

    @GetMapping("/managers/{managerId}/drivers")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "List all drivers for a fleet manager")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getDrivers(@PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                fleetManagementService.getDriversByManager(managerId)));
    }

    @GetMapping("/drivers/{driverId}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get a driver by ID")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriver(@PathVariable UUID driverId) {
        return ResponseEntity.ok(ApiResponse.ok(fleetManagementService.getDriverById(driverId)));
    }

    // ===== Vehicles =====

    @PostMapping("/managers/{managerId}/vehicles")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Add a vehicle to fleet")
    public ResponseEntity<ApiResponse<VehicleResponse>> addVehicle(
            @PathVariable UUID managerId,
            @Valid @RequestBody AddVehicleRequest request) {
        VehicleResponse response = fleetManagementService.addVehicle(managerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Vehicle added", response));
    }

    @DeleteMapping("/vehicles/{vehicleId}")
    @PreAuthorize("hasRole('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Soft-delete a vehicle")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable UUID vehicleId) {
        fleetManagementService.deleteVehicle(vehicleId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Vehicle deleted").build());
    }

    @GetMapping("/managers/{managerId}/vehicles")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "List vehicles for a fleet manager")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getVehicles(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                fleetManagementService.getVehiclesByManager(managerId)));
    }

    // ===== Dependents =====

    @PostMapping("/dependents")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER')")
    @Operation(summary = "Add emergency contact for driver")
    public ResponseEntity<ApiResponse<DependentResponse>> addDependent(
            @Valid @RequestBody CreateDependentRequest request) {
        DependentResponse response = fleetManagementService.addDependent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Dependent added", response));
    }

    @GetMapping("/drivers/{driverId}/dependents")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_DRIVER')")
    @Operation(summary = "Get emergency contacts for a driver")
    public ResponseEntity<ApiResponse<List<DependentResponse>>> getDependents(
            @PathVariable UUID driverId) {
        return ResponseEntity.ok(ApiResponse.ok(
                fleetManagementService.getDependentsByDriver(driverId)));
    }
}
