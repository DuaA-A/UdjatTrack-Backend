package com.udjattrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {
    private long totalTrips;
    private long activeTrips;
    private long finishedTrips;
    private long cancelledTrips;
    
    private long totalAlerts;
    private Map<String, Long> alertsBySeverity;
    
    private long openIssues;
    private long resolvedIssues;
    
    private long totalIncidents;
}
