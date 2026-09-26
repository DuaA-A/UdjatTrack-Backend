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
import java.nio.charset.StandardCharsets;
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
    public ReportSummaryResponse getFleetSummary(UUID managerId, UUID driverId, UUID vehicleId) {
        List<TripResponse> trips = tripService.getTripsByFleetManager(managerId).stream()
                .filter(t -> driverId == null || t.getDriverId().equals(driverId))
                .filter(t -> vehicleId == null || t.getVehicleId().equals(vehicleId))
                .collect(Collectors.toList());
        List<UUID> tripIds = trips.stream().map(TripResponse::getTripId).collect(Collectors.toList());

        List<AlertResponse> alerts = alertService.getAllAlertsByFleetManager(managerId).stream()
                .filter(a -> (driverId == null && vehicleId == null) || (a.getTripId() != null && tripIds.contains(a.getTripId())))
                .collect(Collectors.toList());

        List<SOSRequestResponse> sos = emergencyService.getSOSRequestsByFleetManager(managerId).stream()
                .filter(s -> (driverId == null && vehicleId == null) || tripIds.contains(s.getTripId()))
                .collect(Collectors.toList());

        List<MaintenanceRequestResponse> maintenance = emergencyService.getMaintenanceRequestsByFleetManager(managerId).stream()
                .filter(m -> (driverId == null && vehicleId == null) || tripIds.contains(m.getTripId()))
                .collect(Collectors.toList());

        List<IncidentResponse> incidents = emergencyService.getIncidentsByFleetManager(managerId).stream()
                .filter(i -> (driverId == null && vehicleId == null) || tripIds.contains(i.getTripId()))
                .collect(Collectors.toList());

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
    public ByteArrayInputStream exportFleetSummaryToPdf(UUID managerId, UUID driverId, UUID vehicleId) {
        ReportSummaryResponse data = getFleetSummary(managerId, driverId, vehicleId);
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

            Paragraph title = new Paragraph("UdjatTrack Fleet Operational Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), normalFont));
            document.add(new Paragraph("Manager ID: " + managerId.toString(), normalFont));
            if (driverId != null) document.add(new Paragraph("Filtered by Driver: " + driverId.toString(), normalFont));
            if (vehicleId != null) document.add(new Paragraph("Filtered by Vehicle: " + vehicleId.toString(), normalFont));
            document.add(Chunk.NEWLINE);
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

            document.add(new Paragraph("Security & Safety Alerts", subTitleFont));
            PdfPTable alertTable = new PdfPTable(2);
            alertTable.setWidthPercentage(100);
            alertTable.setSpacingBefore(10f);

            addTableCell(alertTable, "Total Alerts Triggered", normalFont);
            addTableCell(alertTable, String.valueOf(data.getTotalAlerts()), normalFont);
            addTableCell(alertTable, "Total Incidents (Accidents/Events)", normalFont);
            addTableCell(alertTable, String.valueOf(data.getTotalIncidents()), normalFont);

            document.add(alertTable);
            
            if (data.getAlertsBySeverity() != null && !data.getAlertsBySeverity().isEmpty()) {
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

    @Override
    public ByteArrayInputStream exportFleetSummaryToCsv(UUID managerId, UUID driverId, UUID vehicleId) {
        ReportSummaryResponse data = getFleetSummary(managerId, driverId, vehicleId);
        StringBuilder sb = new StringBuilder();
        
        sb.append("Metric,Value\n");
        sb.append("Total Trips,").append(data.getTotalTrips()).append("\n");
        sb.append("Active Trips,").append(data.getActiveTrips()).append("\n");
        sb.append("Finished Trips,").append(data.getFinishedTrips()).append("\n");
        sb.append("Cancelled Trips,").append(data.getCancelledTrips()).append("\n");
        sb.append("Total Alerts,").append(data.getTotalAlerts()).append("\n");
        sb.append("Total Incidents,").append(data.getTotalIncidents()).append("\n");
        sb.append("Open Issues,").append(data.getOpenIssues()).append("\n");
        sb.append("Resolved Issues,").append(data.getResolvedIssues()).append("\n");
        
        if (data.getAlertsBySeverity() != null && !data.getAlertsBySeverity().isEmpty()) {
            sb.append("\nAlert Severity,Count\n");
            for (Map.Entry<String, Long> entry : data.getAlertsBySeverity().entrySet()) {
                sb.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
            }
        }

        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<TripResponse> getFleetSummaryTable(UUID managerId, UUID driverId, UUID vehicleId) {
        return tripService.getTripsByFleetManager(managerId).stream()
                .filter(t -> driverId == null || t.getDriverId().equals(driverId))
                .filter(t -> vehicleId == null || t.getVehicleId().equals(vehicleId))
                .collect(Collectors.toList());
    }

    @Override
    public java.util.Map<String, Long> getKpis(UUID managerId, String dateRange) {
        ReportSummaryResponse summary = getFleetSummary(managerId, null, null);
        
        List<AlertResponse> allAlerts = alertService.getAllAlertsByFleetManager(managerId);
        long fatigueCount = allAlerts.stream()
                .filter(a -> "Fatigue".equalsIgnoreCase(a.getAlertType().name()) 
                          || "DROWSINESS".equalsIgnoreCase(a.getAlertType().name()))
                .count();
        
        List<SOSRequestResponse> sosRequests = emergencyService.getSOSRequestsByFleetManager(managerId);
        
        java.util.Map<String, Long> kpis = new java.util.HashMap<>();
        kpis.put("tripsCompleted", summary.getFinishedTrips());
        kpis.put("fatigueAlerts", fatigueCount);
        kpis.put("sosAlerts", (long) sosRequests.size());
        
        return kpis;
    }

    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    @Override
    public ByteArrayInputStream generateTripReportPdf(UUID tripId) {
        TripResponse trip = tripService.getTripReport(tripId);
        
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);

            Paragraph title = new Paragraph("Trip Safety & Performance Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), normalFont));
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Trip Overview", subTitleFont));
            PdfPTable overviewTable = new PdfPTable(2);
            overviewTable.setWidthPercentage(100);
            overviewTable.setSpacingBefore(10f);

            addTableCell(overviewTable, "Trip ID", normalFont);
            addTableCell(overviewTable, tripId.toString(), normalFont);
            addTableCell(overviewTable, "Driver", normalFont);
            addTableCell(overviewTable, trip.getDriverName() != null ? trip.getDriverName() : "N/A", normalFont);
            addTableCell(overviewTable, "Vehicle", normalFont);
            addTableCell(overviewTable, trip.getVehiclePlate() != null ? trip.getVehiclePlate() : "N/A", normalFont);
            addTableCell(overviewTable, "Route", normalFont);
            addTableCell(overviewTable, trip.getSource() + " → " + trip.getDestination(), normalFont);
            addTableCell(overviewTable, "Status", normalFont);
            addTableCell(overviewTable, trip.getStatus().name(), normalFont);

            document.add(overviewTable);
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Trip Timeline", subTitleFont));
            PdfPTable timeTable = new PdfPTable(2);
            timeTable.setWidthPercentage(100);
            timeTable.setSpacingBefore(10f);

            addTableCell(timeTable, "Scheduled Start", normalFont);
            addTableCell(timeTable, trip.getScheduledStartTime() != null ? trip.getScheduledStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A", normalFont);
            addTableCell(timeTable, "Actual Start", normalFont);
            addTableCell(timeTable, trip.getActualStartTime() != null ? trip.getActualStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A", normalFont);
            addTableCell(timeTable, "Scheduled End", normalFont);
            addTableCell(timeTable, trip.getScheduledEndTime() != null ? trip.getScheduledEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A", normalFont);
            addTableCell(timeTable, "Actual End", normalFont);
            addTableCell(timeTable, trip.getActualEndTime() != null ? trip.getActualEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "N/A", normalFont);

            if (trip.getTripLog() != null) {
                addTableCell(timeTable, "Total Duration (hours)", normalFont);
                addTableCell(timeTable, String.format("%.2f", trip.getTripLog().getTotalDuration()), normalFont);
                addTableCell(timeTable, "Break Time (hours)", normalFont);
                addTableCell(timeTable, String.format("%.2f", trip.getTripLog().getTotalBreakTime()), normalFont);
            }

            document.add(timeTable);
            document.add(Chunk.NEWLINE);

            if (trip.getTripState() != null) {
                document.add(new Paragraph("Safety & State Information", subTitleFont));
                PdfPTable stateTable = new PdfPTable(2);
                stateTable.setWidthPercentage(100);
                stateTable.setSpacingBefore(10f);

                addTableCell(stateTable, "Driver State", normalFont);
                addTableCell(stateTable, trip.getTripState().getDriverState() != null ? trip.getTripState().getDriverState().name() : "N/A", normalFont);
                addTableCell(stateTable, "Trip Progress State", normalFont);
                addTableCell(stateTable, trip.getTripState().getTripProgressState() != null ? trip.getTripState().getTripProgressState().name() : "N/A", normalFont);
                addTableCell(stateTable, "Current Speed (last known)", normalFont);
                addTableCell(stateTable, trip.getTripState().getCurrentSpeed() != null ? String.format("%.2f km/h", trip.getTripState().getCurrentSpeed()) : "N/A", normalFont);

                document.add(stateTable);
                document.add(Chunk.NEWLINE);
            }

            document.close();
        } catch (DocumentException e) {
            log.error("Error generating trip PDF report: {}", e.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
