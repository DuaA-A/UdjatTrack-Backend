package com.udjattrack.dto.request;

import com.udjattrack.entity.enums.DriverState;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.OffsetDateTime;
import java.util.Map;

public record TelemetryRequest(

        @NotNull(message = "Timestamp is required")
        @JsonProperty("timeStamp")
        @JsonAlias({"timestamp", "timestamp"})
        OffsetDateTime timeStamp,

        LocationDTO location,

        @JsonProperty("speedKmh")
        Double speed,

        DriverState driverState,

        Map<String, Object> payload
) {}
