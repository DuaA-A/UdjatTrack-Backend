package com.udjattrack.dto.request;

import com.udjattrack.entity.enums.IncidentType;
import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateIncidentRequest(

        @NotNull(message = "Trip ID is required")
        UUID tripId,

        @NotNull(message = "Incident type is required")
        IncidentType type,

        @NotNull(message = "Severity is required")
        SeverityLevel severity,

        String location,

        Map<String, Object> payload
) {}
