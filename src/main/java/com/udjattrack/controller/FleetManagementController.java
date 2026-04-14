package com.udjattrack.controller;

import com.udjattrack.dto.request.UpdateFleetManagerRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.FleetManagerResponse;
import com.udjattrack.service.FleetManagementService;
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
@RequestMapping("/fleet-managers")
@RequiredArgsConstructor
@Tag(name = "Fleet Manager Admin", description = "Administrative operations for fleet manager accounts (Super Manager only)")
public class FleetManagementController {

    private final FleetManagementService fleetManagementService;

    @PutMapping("/{managerId}")
    @PreAuthorize("hasRole('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Update a fleet manager")
    public ResponseEntity<ApiResponse<FleetManagerResponse>> updateFleetManager(
            @PathVariable UUID managerId,
            @Valid @RequestBody UpdateFleetManagerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Manager updated",
                fleetManagementService.updateFleetManager(managerId, request)));
    }

    @DeleteMapping("/{managerId}")
    @PreAuthorize("hasRole('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Soft-delete a fleet manager")
    public ResponseEntity<ApiResponse<Void>> deleteFleetManager(@PathVariable UUID managerId) {
        fleetManagementService.deleteFleetManager(managerId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Fleet manager deleted").build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_SUPER_MANAGER')")
    @Operation(summary = "List all fleet managers")
    public ResponseEntity<ApiResponse<List<FleetManagerResponse>>> getAllManagers() {
        return ResponseEntity.ok(ApiResponse.ok(fleetManagementService.getAllFleetManagers()));
    }

    @GetMapping("/{managerId}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_MANAGER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Get fleet manager by ID")
    public ResponseEntity<ApiResponse<FleetManagerResponse>> getManager(
            @PathVariable UUID managerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                fleetManagementService.getFleetManagerById(managerId)));
    }
}
