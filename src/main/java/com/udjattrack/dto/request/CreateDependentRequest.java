package com.udjattrack.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateDependentRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150)
        String name,

        @NotBlank(message = "Phone number is required")
        @Size(max = 20)
        String phoneNumber,

        @Size(max = 50)
        String relation
) {}

