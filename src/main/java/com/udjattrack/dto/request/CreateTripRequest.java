package com.udjattrack.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateTripRequest(

        @NotNull(message = "Driver ID is required")
        UUID driverId,

        @NotNull(message = "Vehicle ID is required")
        UUID vehicleId,

        @NotBlank(message = "Source is required")
        String source,

        @NotBlank(message = "Destination is required")
        String destination,

        String routeName,

        @NotNull(message = "Scheduled start time is required")
        LocalDateTime scheduledStartTime,

        @NotNull(message = "Scheduled end time is required")
        LocalDateTime scheduledEndTime
) {}
