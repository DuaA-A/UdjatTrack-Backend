package com.udjattrack.dto.request;

import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAlertRequest(

        @NotNull(message = "Trip ID is required")
        UUID tripId,

        @NotNull(message = "Alert type is required")
        AlertType alertType,

        @NotNull(message = "Severity is required")
        SeverityLevel severity,

        String alertableType,

        UUID alertableId,

        String message
) {}
