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
    private AlertType alertType;
    private SeverityLevel severity;
    private LocalDateTime timeStamp;
    private Boolean acknowledged;
    private Boolean readByManager;
    private LocalDateTime ackedAt;
    private String message;
    private String alertableType;
    private UUID alertableId;

    // Simple flat info — NO entity references
    private DriverInfo driver;
    private VehicleInfo vehicle;

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