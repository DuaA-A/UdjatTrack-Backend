package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateIncidentRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.IncidentResponse;
import com.udjattrack.service.EmergencyService;
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
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Historical accidents and near-misses reporting")
public class IncidentController {

    private final EmergencyService emergencyService;

    @GetMapping("/incidents")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Lists historical accidents/near-misses for the Alerts & Incidents page")
    public ResponseEntity<ApiResponse<List<IncidentResponse>>> listIncidents(
            @RequestParam(required = false) UUID managerId) {
        if (managerId != null) {
            return ResponseEntity.ok(ApiResponse.ok(emergencyService.getIncidentsByFleetManager(managerId)));
        }
        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }

    @PostMapping("/trips/{tripId}/incidents")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    @Operation(summary = "POST Incident (Driver manually reports)")
    public ResponseEntity<ApiResponse<IncidentResponse>> reportIncident(
            @PathVariable UUID tripId,
            @Valid @RequestBody CreateIncidentRequest request) {
        CreateIncidentRequest finalRequest = new CreateIncidentRequest(
                tripId, 
                request.type(), 
                request.severity(), 
                request.location(), 
                request.payload()
        );
        
        IncidentResponse response = emergencyService.createIncident(finalRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Incident reported", response));
    }
}
