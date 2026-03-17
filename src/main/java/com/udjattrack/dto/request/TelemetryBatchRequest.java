package com.udjattrack.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TelemetryBatchRequest(

        @NotEmpty(message = "Batch must contain at least one record")
        @Valid
        List<TelemetryRequest> records
) {}
