package com.udjattrack.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateTripRequest(
        UUID driverId,
        UUID vehicleId,
        String source,
        String destination,
        LocalDateTime scheduledStartTime,
        LocalDateTime scheduledEndTime
) {}
