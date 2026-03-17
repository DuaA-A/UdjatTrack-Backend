package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.TripProgressState;
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
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private TripProgressState tripState;
    private LocalDateTime createdAt;
    private TripLogResponse tripLog;
}
