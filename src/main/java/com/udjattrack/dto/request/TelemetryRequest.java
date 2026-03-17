package com.udjattrack.dto.request;

import com.udjattrack.entity.enums.DriverState;
import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record TelemetryRequest(

        @NotNull(message = "Trip ID is required")
        UUID tripId,

        Double speed,

        String location,

        DriverState driverState,

        SeverityLevel severity,

        @NotBlank(message = "Event type is required")
        String eventType,

        Map<String, Object> details
) {}
