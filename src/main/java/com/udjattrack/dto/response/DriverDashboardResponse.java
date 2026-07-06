package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DriverDashboardResponse {
    private TripResponse currentTrip; 
    private TripResponse todaysTrip; 
    private TripResponse previousTrip;
    private List<TripResponse> upcomingTrips;
}
