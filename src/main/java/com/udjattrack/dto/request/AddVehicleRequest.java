package com.udjattrack.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddVehicleRequest(

        @NotBlank(message = "Plate number is required")
        @Size(max = 20)
        String plateNumber,

        @NotBlank(message = "Model is required")
        @Size(max = 100)
        String model,

        @NotNull(message = "Manufacture year is required")
        @Positive
        Integer manufactureYear
) {}
