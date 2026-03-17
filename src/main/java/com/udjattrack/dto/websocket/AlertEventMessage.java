package com.udjattrack.dto.websocket;

import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.SeverityLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AlertEventMessage {
    private UUID alertId;
    private UUID tripId;
    private AlertType type;
    private SeverityLevel severity;
    private String message;
    private LocalDateTime timestamp;
}
