package com.udjattrack.dto.request;

import com.udjattrack.entity.enums.DriverState;
import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record TelemetryRequest(

        @NotNull(message = "Trip ID is required")
        UUID tripId,

        @NotNull(message = "Timestamp is required")
        @JsonProperty("timeStamp")
        OffsetDateTime timeStamp,

        LocationDTO location,

        Double speed,

        DriverState driverState,

        Map<String, Object> details
) {}
