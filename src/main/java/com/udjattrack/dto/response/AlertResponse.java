package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.SeverityLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AlertResponse {
    private UUID alertId;
    private UUID tripLogId;
    private AlertType alertType;
    private SeverityLevel severity;
    private Boolean acknowledged;
    private String message;
    private LocalDateTime timestamp;
}
