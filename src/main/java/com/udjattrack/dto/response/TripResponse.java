package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.TripStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TripResponse {
    private UUID tripId;
    private UUID driverId;
    private String driverName;
    private UUID vehicleId;
    private String vehiclePlate;
    private String source;
    private String destination;
    private String routeName;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private TripStatus status;
    private LocalDateTime createdAt;
    private TripLogResponse tripLog;

    private String title;
    private Double expectedDurationHours;
    private LocalDateTime estimatedArrivalTime;
    private Integer progressPct;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private TripStateResponse tripState;
    private VehicleInfo vehicle;
    private DriverInfo driver;

    @Getter
    @Builder
    public static class VehicleInfo {
        private String plateNumber;
        private String model;
    }

    @Getter
    @Builder
    public static class DriverInfo {
        private String name;
        private String phoneNumber;
    }
}
