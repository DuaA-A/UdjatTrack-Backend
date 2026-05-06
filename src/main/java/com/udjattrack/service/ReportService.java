package com.udjattrack.service;

import com.udjattrack.dto.response.ReportSummaryResponse;
import java.io.ByteArrayInputStream;
import java.util.UUID;

public interface ReportService {
    ReportSummaryResponse getFleetSummary(UUID managerId);
    ByteArrayInputStream exportFleetSummaryToPdf(UUID managerId);
}
