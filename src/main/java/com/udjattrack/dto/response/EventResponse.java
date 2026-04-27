package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class EventResponse {
    private UUID eventId;
    private UUID tripId;
    private String eventType;
    private com.udjattrack.entity.enums.SeverityLevel severity;
    private java.util.Map<String, Object> payload;
    private java.time.LocalDateTime timestamp;
    private boolean alertCreated;
}
