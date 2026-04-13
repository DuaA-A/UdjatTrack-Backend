package com.udjattrack.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectFleetManagerRequest(
        @NotBlank(message = "Rejection reason is required")
        String reason
) {}
