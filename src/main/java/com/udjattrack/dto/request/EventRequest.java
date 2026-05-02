package com.udjattrack.dto.request;

import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record EventRequest(
    
    @NotNull(message = "Timestamp is required")
    @JsonProperty("timeStamp")
    @JsonAlias({"timestamp", "timestamp"})
    OffsetDateTime timeStamp,
    
    @NotBlank(message = "Event type is required")
    String eventType,
    
    @NotNull(message = "Severity is required")
    SeverityLevel severity,
    
    Map<String, Object> payload
) {}
