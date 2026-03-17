package com.udjattrack.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateFleetManagerRequest(

        @Size(max = 150)
        String name,

        @Size(max = 200)
        String companyName,

        String subscriptionPlan
) {}
