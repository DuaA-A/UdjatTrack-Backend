package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateDependentRequest;
import com.udjattrack.dto.request.CreateDriverRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.DependentResponse;
import com.udjattrack.dto.response.DriverResponse;
import com.udjattrack.service.FleetManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver Management", description = "Manage driver accounts and emergency contacts")
public class DriverController {

    private final FleetManagementService fleetManagementService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Create a new driver account and assign to fleet")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(
            @Valid @RequestBody CreateDriverRequest request) {
        UUID managerId = com.udjattrack.util.SecurityUtils.getCurrentUserId();
        DriverResponse response = fleetManagementService.createDriver(managerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Driver created", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    @Operation(summary = "Get the authenticated driver's own profile")
    public ResponseEntity<ApiResponse<DriverResponse>> getMe() {
        UUID driverId = com.udjattrack.util.SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(fleetManagementService.getDriverById(driverId)));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get details of a specific driver by ID")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(fleetManagementService.getDriverById(id)));
    }


    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "List all drivers in the fleet")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getAllDrivers() {
        UUID managerId = com.udjattrack.util.SecurityUtils.getCurrentUserId();
        // SuperManager might want to see all drivers, but for now we follow fleet manager context
        return ResponseEntity.ok(ApiResponse.ok(fleetManagementService.getDriversByManager(managerId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_FLEET_MANAGER')")
    @Operation(summary = "Remove a driver from the fleet")
    public ResponseEntity<ApiResponse<Void>> deleteDriver(@PathVariable UUID id) {
        fleetManagementService.deleteDriver(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Driver deleted").build());
    }

    // Emergency Contacts (Section 4 in design, mapped to /drivers/{id}/emergency-contacts)
    
    @PostMapping("/{driverId}/emergency-contacts")
    @PreAuthorize("hasAnyAuthority('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Adds a new dependent to the driver's profile")
    public ResponseEntity<ApiResponse<DependentResponse>> addEmergencyContact(
            @PathVariable UUID driverId,
            @Valid @RequestBody CreateDependentRequest request) {
        // Logic to ensure driverId matches request or caller has authority
        DependentResponse response = fleetManagementService.addDependent(driverId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Emergency contact added", response));
    }

    @GetMapping("/{driverId}/emergency-contacts")
    @PreAuthorize("hasAnyAuthority('ROLE_DRIVER', 'ROLE_FLEET_MANAGER')")
    @Operation(summary = "Retrieves a list of the driver's emergency contacts")
    public ResponseEntity<ApiResponse<List<DependentResponse>>> getEmergencyContacts(
            @PathVariable UUID driverId) {
        return ResponseEntity.ok(ApiResponse.ok(
                fleetManagementService.getDependentsByDriver(driverId)));
    }

    /**
     * POST /drivers/{id}/photo
     *
     * <p>Uploads a profile photo for the given driver.
     * Consumes multipart/form-data; the file must be sent as a form field named "file".
     *
     * <p>Allowed types: JPEG, PNG, WebP — max 5 MB.
     *
     * <p>Example curl:
     * <pre>
     *   curl -X POST http://localhost:8080/api/v1/drivers/{id}/photo \
     *        -H "Authorization: Bearer <token>" \
     *        -F "file=@/path/to/photo.jpg"
     * </pre>
     */
    @PostMapping(
            value = "/{id}/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_DRIVER')")
    @Operation(summary = "Upload or replace a driver's profile photo (multipart/form-data)")
    public ResponseEntity<ApiResponse<DriverResponse>> uploadDriverPhoto(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        DriverResponse response = fleetManagementService.uploadDriverPhoto(id, file);
        return ResponseEntity.ok(ApiResponse.ok("Photo uploaded successfully", response));
    }
}
