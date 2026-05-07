package com.udjattrack.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateDriverRequest(

        @Size(max = 150)
        String name,

        @JsonProperty("phoneNumber")
        @JsonAlias("phone")
        @Size(max = 20)
        String phoneNumber,

        @Size(max = 50)
        String licenseNumber
) {}
