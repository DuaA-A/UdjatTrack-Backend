package com.udjattrack.dto.request;

import java.util.List;

public record OfflineSyncRequest(
    List<TelemetryRequest> telemetryRecords,
    List<EventRequest> events
) {}
