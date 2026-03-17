package com.udjattrack.dto.websocket;

import com.udjattrack.entity.enums.DriverState;
import com.udjattrack.entity.enums.TripProgressState;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TripStateUpdateMessage {
    private UUID tripId;
    private DriverState driverState;
    private TripProgressState tripProgressState;
    private String latitude;
    private String longitude;
    private Double currentSpeed;
    private LocalDateTime timestamp;
}
