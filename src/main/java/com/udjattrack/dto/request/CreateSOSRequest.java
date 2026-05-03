package com.udjattrack.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateSOSRequest(

        UUID tripId,

        String location,

        Boolean autoTriggered,

        Map<String, Object> payload
) {}
