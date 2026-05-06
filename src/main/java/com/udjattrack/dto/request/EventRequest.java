package com.udjattrack.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.OffsetDateTime;
import java.util.Map;

public record EventRequest(
    
    @NotNull(message = "Timestamp is required")
    @JsonProperty("timeStamp")
    @JsonAlias({"timestamp", "timestamp"})
    OffsetDateTime timeStamp,
    
    @NotBlank(message = "Event type is required")
    String eventType,
    
    Map<String, Object> payload
) {}

