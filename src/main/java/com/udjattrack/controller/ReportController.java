package com.udjattrack.controller;

import com.udjattrack.dto.response.*;
import com.udjattrack.service.*;
import com.udjattrack.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Aggregated operational summaries and analytics")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get high-level KPIs for reports dashboard")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getKpis(
            @RequestParam(required = false, defaultValue = "LAST_YEAR") String dateRange) {
        UUID managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok("KPIs retrieved", reportService.getKpis(managerId, dateRange)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get high-level operational summary for the fleet")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getFleetSummary(
            @RequestParam(required = false) UUID driverId,
            @RequestParam(required = false) UUID vehicleId) {
        UUID managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getFleetSummary(managerId, driverId, vehicleId)));
    }

    @GetMapping("/table")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get detailed report summary table for the fleet")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getFleetSummaryTable(
            @RequestParam(required = false) UUID driverId,
            @RequestParam(required = false) UUID vehicleId) {
        UUID managerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getFleetSummaryTable(managerId, driverId, vehicleId)));
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Export fleet operational summary to PDF")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportToPdf(
            @RequestParam(required = false) UUID driverId,
            @RequestParam(required = false) UUID vehicleId) {
        UUID managerId = SecurityUtils.getCurrentUserId();
        java.io.ByteArrayInputStream bis = reportService.exportFleetSummaryToPdf(managerId, driverId, vehicleId);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=fleet-report-" + managerId + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(new org.springframework.core.io.InputStreamResource(bis));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Export fleet operational summary to CSV")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportToCsv(
            @RequestParam(required = false) UUID driverId,
            @RequestParam(required = false) UUID vehicleId) {
        UUID managerId = SecurityUtils.getCurrentUserId();
        java.io.ByteArrayInputStream bis = reportService.exportFleetSummaryToCsv(managerId, driverId, vehicleId);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=fleet-report-" + managerId + ".csv");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(new org.springframework.core.io.InputStreamResource(bis));
    }
        @GetMapping("/trip/{tripId}/download")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_DRIVER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Download trip report as PDF")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> downloadTripReportPdf(
            @PathVariable UUID tripId) {
        java.io.ByteArrayInputStream bis = reportService.generateTripReportPdf(tripId);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=trip-report-" + tripId + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(new org.springframework.core.io.InputStreamResource(bis));
    }

    @GetMapping("/driver/{driverId}/download")
    @PreAuthorize("hasAnyAuthority('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Download driver safety performance report as PDF")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> downloadDriverReportPdf(
            @PathVariable UUID driverId) {
        UUID managerId = SecurityUtils.getCurrentUserId();
        // Uses the fleet summary generator but filtered entirely by this driverId
        java.io.ByteArrayInputStream bis = reportService.exportFleetSummaryToPdf(managerId, driverId, null);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=driver-report-" + driverId + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(new org.springframework.core.io.InputStreamResource(bis));
    }
}
