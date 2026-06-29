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
    private UUID tripId;
    private AlertType alertType;
    private SeverityLevel severity;
    private Boolean acknowledged;

    private LocalDateTime acknowledgedAt;
    private String message;
    private String alertableType;
    private UUID alertableId;
    private LocalDateTime timestamp;
    private TripInfo trip;
    private DriverInfo driver;
    private VehicleInfo vehicle;
    private Object event;
    
    @Getter
    @Builder
    public static class TripInfo {
        private UUID tripId;
        private String title;
        private String status;
        private LocalDateTime scheduledStartTime;
        private LocalDateTime scheduledEndTime;
        private String source;
        private String destination;
    }
    @Getter
    @Builder
    public static class DriverInfo {
        private UUID driverId;
        private String name;
        private String phone;
        private String currentState;
    }
    @Getter
    @Builder
    public static class VehicleInfo {
        private UUID vehicleId;
        private String plateNumber;
        private String model;
    }
}
