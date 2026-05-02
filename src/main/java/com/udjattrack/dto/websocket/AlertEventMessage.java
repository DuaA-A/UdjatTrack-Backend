package com.udjattrack.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.SeverityLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AlertEventMessage {
    private UUID alertId;
    private UUID tripId;
    private AlertType alertType;
    private SeverityLevel severity;
    
    @JsonProperty("timeStamp")
    private LocalDateTime timeStamp;
    
    private boolean acknowledged;
    private boolean readByManager;
    private LocalDateTime ackedAt;
    private String message;
    
    private DriverInfo driver;
    private VehicleInfo vehicle;
    private Object alertable;

    @Getter
    @Builder
    public static class DriverInfo {
        private UUID driverId;
        private String name;
    }

    @Getter
    @Builder
    public static class VehicleInfo {
        private String plateNumber;
        private String model;
    }
}
