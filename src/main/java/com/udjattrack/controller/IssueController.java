package com.udjattrack.controller;

import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.MaintenanceRequestResponse;
import com.udjattrack.dto.response.SOSRequestResponse;
import com.udjattrack.service.EmergencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/issues")
@RequiredArgsConstructor
@Tag(name = "Issues", description = "SOS and Maintenance tickets that require human intervention")
public class IssueController {

    private final EmergencyService emergencyService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Lists SOS and Maintenance tickets")
    public ResponseEntity<ApiResponse<List<Object>>> listIssues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        // Simple aggregation stub
        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }

    @PostMapping("/{issueId}/resolve")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Marks an SOS or maintenance ticket as completed")
    public ResponseEntity<ApiResponse<Void>> resolveIssue(
            @PathVariable UUID issueId,
            @RequestBody String resolutionNote) {
        // Implementation placeholder
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Issue resolved successfully")
                .build());
    }
}
