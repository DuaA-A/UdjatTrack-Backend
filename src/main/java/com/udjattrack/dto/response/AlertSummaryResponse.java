package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlertSummaryResponse {
    private long totalAlerts;
    private long acknowledgedCount;
    private long unacknowledgedCount;
    private long criticalCount;
    private long highCount;
    private long mediumCount;
    private long lowCount;
}
