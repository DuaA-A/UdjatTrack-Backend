package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TripLogResponse {
    private UUID logId;
    private UUID tripId;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Double totalDuration;
    private Double totalBreakTime;
}
