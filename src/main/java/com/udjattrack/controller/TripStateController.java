package com.udjattrack.controller;

import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.service.TripService;
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
@RequestMapping("/trip-states")
@RequiredArgsConstructor
@Tag(name = "Trip State", description = "Real-time trip state queries")
public class TripStateController {

    private final TripService tripService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get active trip states for fleet manager's drivers")
    public ResponseEntity<ApiResponse<List<TripStateResponse>>> getActiveTripStates(
            @RequestParam(required = false, defaultValue = "active") String status) {
        UUID managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("Active trip states retrieved",
                tripService.getActiveTripStates(managerId)));
    }
}
