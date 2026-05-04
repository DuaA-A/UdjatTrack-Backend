package com.udjattrack.controller;

import com.udjattrack.dto.response.*;
import com.udjattrack.entity.enums.IssueStatus;
import com.udjattrack.service.*;
import com.udjattrack.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Aggregated operational summaries and analytics")
public class ReportController {

    private final TripService tripService;
    private final AlertService alertService;
    private final EmergencyService emergencyService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get high-level operational summary for the fleet")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getFleetSummary() {
        UUID managerId = SecurityUtils.getCurrentUserId();

        // 1. Fetch data
        List<TripResponse> trips = tripService.getTripsByFleetManager(managerId);
        List<AlertResponse> alerts = alertService.getAllAlertsByFleetManager(managerId);
        List<SOSRequestResponse> sos = emergencyService.getSOSRequestsByFleetManager(managerId);
        List<MaintenanceRequestResponse> maintenance = emergencyService.getMaintenanceRequestsByFleetManager(managerId);
        List<IncidentResponse> incidents = emergencyService.getIncidentsByFleetManager(managerId);

        // 2. Aggregate
        Map<String, Long> alertCounts = alerts.stream()
                .collect(Collectors.groupingBy(a -> a.getSeverity().name(), Collectors.counting()));

        long finishedTrips = trips.stream().filter(t -> "FINISHED".equalsIgnoreCase(t.getStatus().name())).count();
        long activeTrips = trips.stream().filter(t -> List.of("ONGOING", "ON_BREAK").contains(t.getStatus().name())).count();
        long cancelledTrips = trips.stream().filter(t -> "CANCELLED".equalsIgnoreCase(t.getStatus().name())).count();

        long openIssues = sos.stream().filter(s -> s.getStatus() != IssueStatus.RESOLVED).count() +
                         maintenance.stream().filter(m -> m.getStatus() != IssueStatus.RESOLVED).count();
        long resolvedIssues = (sos.size() + maintenance.size()) - openIssues;

        ReportSummaryResponse summary = ReportSummaryResponse.builder()
                .totalTrips(trips.size())
                .activeTrips(activeTrips)
                .finishedTrips(finishedTrips)
                .cancelledTrips(cancelledTrips)
                .totalAlerts(alerts.size())
                .alertsBySeverity(alertCounts)
                .openIssues(openIssues)
                .resolvedIssues(resolvedIssues)
                .totalIncidents(incidents.size())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
