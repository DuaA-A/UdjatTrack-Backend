package com.udjattrack.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record OfflineSyncRequest(
    @NotNull(message = "Trip ID is required for offline sync")
    UUID tripId,
    List<TelemetryRequest> telemetryRecords,
    List<EventRequest> events
) {}
