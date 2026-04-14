package com.udjattrack.dto.request;

import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record EventRequest(
    UUID id,
    
    @NotNull(message = "Timestamp is required")
    LocalDateTime timestamp,
    
    @NotBlank(message = "Event type is required")
    String eventType,
    
    @NotNull(message = "Severity is required")
    SeverityLevel severity,
    
    Map<String, Object> payload
) {}
