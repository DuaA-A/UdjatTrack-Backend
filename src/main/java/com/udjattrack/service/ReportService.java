package com.udjattrack.service;

import com.udjattrack.dto.response.ReportSummaryResponse;
import com.udjattrack.dto.response.TripResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

public interface ReportService {
    ReportSummaryResponse getFleetSummary(UUID managerId, UUID driverId, UUID vehicleId);
    ByteArrayInputStream exportFleetSummaryToPdf(UUID managerId, UUID driverId, UUID vehicleId);
    ByteArrayInputStream exportFleetSummaryToCsv(UUID managerId, UUID driverId, UUID vehicleId);
    List<TripResponse> getFleetSummaryTable(UUID managerId, UUID driverId, UUID vehicleId);
}
