package com.udjattrack.dto.websocket;

import com.udjattrack.entity.enums.DriverState;
import com.udjattrack.entity.enums.TripProgressState;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class TripStateUpdateMessage {
    private UUID tripStateId;
    private UUID tripId;
    private DriverState driverState;
    private TripProgressState tripProgressState;
    private Integer progressPct;
    private Map<String, String> location;
    private LocalDateTime lastUpdatedAt;
}
