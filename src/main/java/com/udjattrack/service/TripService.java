package com.udjattrack.service;

import com.udjattrack.dto.request.CreateTripRequest;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;
import com.udjattrack.dto.response.TripTimelineResponse;

import java.util.List;
import java.util.UUID;

/**
 * Trip service — manages trip lifecycle: create, assign, start, stop, resume, complete.
 */
public interface TripService {

    TripResponse createTrip(CreateTripRequest request);
    TripResponse updateTrip(UUID tripId, com.udjattrack.dto.request.UpdateTripRequest request);
    TripResponse assignDriver(UUID tripId, UUID driverId);
    TripResponse assignVehicle(UUID tripId, UUID vehicleId);
    TripResponse startTrip(UUID tripId);
    TripResponse stopTrip(UUID tripId, com.udjattrack.dto.request.LocationDTO location);
    TripResponse resumeTrip(UUID tripId, com.udjattrack.dto.request.LocationDTO location);
    TripResponse completeTrip(UUID tripId);
    TripResponse cancelTrip(UUID tripId);
    TripStateResponse trackTripProgress(UUID tripId);
    TripResponse getTripById(UUID tripId);
    List<TripResponse> getTripsByDriver(UUID driverId, String status);
    List<TripResponse> getTripsByFleetManager(UUID managerId);
    List<TripResponse> getTripsByFleetManagerWithFilters(UUID managerId, String status, UUID vehicleId, String dateFrom, String dateTo);
    List<TripStateResponse> getActiveTripStates(UUID managerId);
    TripTimelineResponse getTripTimeline(UUID tripId, String from, String to);
    TripResponse getTripReport(UUID tripId);
    com.udjattrack.dto.response.DriverDashboardResponse getDriverDashboard(UUID driverId);
    void publishTripMonitoringUpdate(UUID fleetId);
}
