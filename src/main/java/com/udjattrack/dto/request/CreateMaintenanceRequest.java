package com.udjattrack.dto.request;

import com.udjattrack.entity.enums.MaintenanceType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateMaintenanceRequest(

        @NotNull(message = "Trip ID is required")
        UUID tripId,

        @NotNull(message = "Maintenance type is required")
        MaintenanceType maintenanceType,

        String description,

        Map<String, Object> payload
) {}
