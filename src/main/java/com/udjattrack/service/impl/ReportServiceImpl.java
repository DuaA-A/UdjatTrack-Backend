package com.udjattrack.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.udjattrack.dto.response.*;
import com.udjattrack.entity.enums.IssueStatus;
import com.udjattrack.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final TripService tripService;
    private final AlertService alertService;
    private final EmergencyService emergencyService;

    @Override
    public ReportSummaryResponse getFleetSummary(UUID managerId) {
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

        return ReportSummaryResponse.builder()
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
    }

    @Override
    public ByteArrayInputStream exportFleetSummaryToPdf(UUID managerId) {
        ReportSummaryResponse data = getFleetSummary(managerId);
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Font styles
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

            // Header
            Paragraph title = new Paragraph("UdjatTrack Fleet Operational Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), normalFont));
            document.add(new Paragraph("Manager ID: " + managerId.toString(), normalFont));
            document.add(Chunk.NEWLINE);

            // 1. Trip Statistics Section
            document.add(new Paragraph("Trip Statistics", subTitleFont));
            PdfPTable tripTable = new PdfPTable(2);
            tripTable.setWidthPercentage(100);
            tripTable.setSpacingBefore(10f);
            
            addTableCell(tripTable, "Total Trips", normalFont);
            addTableCell(tripTable, String.valueOf(data.getTotalTrips()), normalFont);
            addTableCell(tripTable, "Active/Ongoing", normalFont);
            addTableCell(tripTable, String.valueOf(data.getActiveTrips()), normalFont);
            addTableCell(tripTable, "Finished", normalFont);
            addTableCell(tripTable, String.valueOf(data.getFinishedTrips()), normalFont);
            addTableCell(tripTable, "Cancelled", normalFont);
            addTableCell(tripTable, String.valueOf(data.getCancelledTrips()), normalFont);
            
            document.add(tripTable);
            document.add(Chunk.NEWLINE);

            // 2. Security & Alerts Section
            document.add(new Paragraph("Security & Safety Alerts", subTitleFont));
            PdfPTable alertTable = new PdfPTable(2);
            alertTable.setWidthPercentage(100);
            alertTable.setSpacingBefore(10f);

            addTableCell(alertTable, "Total Alerts Triggered", normalFont);
            addTableCell(alertTable, String.valueOf(data.getTotalAlerts()), normalFont);
            addTableCell(alertTable, "Total Incidents (Accidents/Events)", normalFont);
            addTableCell(alertTable, String.valueOf(data.getTotalIncidents()), normalFont);

            document.add(alertTable);
            
            // Sub-table for severity
            if (!data.getAlertsBySeverity().isEmpty()) {
                document.add(new Paragraph("Alerts by Severity:", normalFont));
                PdfPTable sevTable = new PdfPTable(2);
                sevTable.setWidthPercentage(60);
                sevTable.setHorizontalAlignment(Element.ALIGN_LEFT);
                for (Map.Entry<String, Long> entry : data.getAlertsBySeverity().entrySet()) {
                    addTableCell(sevTable, entry.getKey(), normalFont);
                    addTableCell(sevTable, String.valueOf(entry.getValue()), normalFont);
                }
                document.add(sevTable);
            }
            document.add(Chunk.NEWLINE);

            // 3. Operational Issues
            document.add(new Paragraph("Operational Issues (SOS & Maintenance)", subTitleFont));
            PdfPTable issueTable = new PdfPTable(2);
            issueTable.setWidthPercentage(100);
            issueTable.setSpacingBefore(10f);

            addTableCell(issueTable, "Open Issues", normalFont);
            addTableCell(issueTable, String.valueOf(data.getOpenIssues()), normalFont);
            addTableCell(issueTable, "Resolved Issues", normalFont);
            addTableCell(issueTable, String.valueOf(data.getResolvedIssues()), normalFont);

            document.add(issueTable);

            document.close();
        } catch (DocumentException e) {
            log.error("Error generating PDF: {}", e.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }
}
