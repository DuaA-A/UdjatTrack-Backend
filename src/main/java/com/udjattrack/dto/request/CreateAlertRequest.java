package com.udjattrack.dto.request;

import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAlertRequest(

        @NotNull(message = "Trip log ID is required")
        UUID tripLogId,

        @NotNull(message = "Alert type is required")
        AlertType alertType,

        @NotNull(message = "Severity is required")
        SeverityLevel severity,

        String message
) {}
