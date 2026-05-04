package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DriverDashboardResponse {
    private TripResponse currentTrip;   // ONGOING or ON_BREAK
    private TripResponse todaysTrip;    // Next PLANNED trip starting today
    private TripResponse previousTrip;  // Most recent FINISHED trip
    private List<TripResponse> upcomingTrips; // All other PLANNED trips
}
