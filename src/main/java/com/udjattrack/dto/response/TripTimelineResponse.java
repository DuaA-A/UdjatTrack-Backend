package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TripTimelineResponse {

    private UUID tripLogId;
    private UUID tripId;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Double totalDurationHours;
    private List<TimelineItem> timeline;

    @Getter
    @Builder
    public static class TimelineItem {
        private LocalDateTime timestamp;
        private String itemType;
        private Boolean isAlert;
        private AlertDetails alertDetails;
        private Object details; // This will hold the specific details map
    }

    @Getter
    @Builder
    public static class AlertDetails {
        private UUID alertId;
        private String severity;
        private Boolean acknowledged;
    }
}
