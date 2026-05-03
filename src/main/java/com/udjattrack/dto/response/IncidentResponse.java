package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.IncidentType;
import com.udjattrack.entity.enums.SeverityLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class IncidentResponse {
    private UUID incidentId;
    private UUID tripId;
    private SeverityLevel severity;
    private IncidentType type;
    private String location;

    private Map<String, Object> payload;
    private LocalDateTime reportedAt;
}
