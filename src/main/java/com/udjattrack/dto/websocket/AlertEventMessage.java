package com.udjattrack.dto.websocket;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AlertEventMessage {

    private UUID alertId;
    private UUID tripId;
    private String alertType;
    private String severity;
    private java.time.OffsetDateTime timeStamp;
    private Boolean acknowledged;
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