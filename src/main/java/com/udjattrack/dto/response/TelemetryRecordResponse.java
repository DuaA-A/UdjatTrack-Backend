package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.DriverState;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class TelemetryRecordResponse {
    private UUID id;
    private UUID tripId;
    private Double speed;
    private String location;
    private DriverState driverState;
    private Map<String, Object> details;
    private LocalDateTime timestamp;
}
