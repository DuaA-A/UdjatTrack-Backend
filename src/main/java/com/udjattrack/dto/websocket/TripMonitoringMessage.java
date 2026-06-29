package com.udjattrack.dto.websocket;

import com.udjattrack.dto.response.TripResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TripMonitoringMessage {
    private long activeTripsCount;
    private List<TripResponse> latestTrips;
}
