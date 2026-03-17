package com.udjattrack.service;

import com.udjattrack.dto.request.CreateTripRequest;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.dto.response.TripStateResponse;

import java.util.List;
import java.util.UUID;

/**
 * Trip service — manages trip lifecycle: create, assign, start, stop, resume, complete.
 */
public interface TripService {

    TripResponse createTrip(CreateTripRequest request);
    TripResponse assignDriver(UUID tripId, UUID driverId);
    TripResponse assignVehicle(UUID tripId, UUID vehicleId);
    TripResponse startTrip(UUID tripId);
    TripResponse stopTrip(UUID tripId);
    TripResponse resumeTrip(UUID tripId);
    TripResponse completeTrip(UUID tripId);
    TripResponse cancelTrip(UUID tripId);
    TripStateResponse trackTripProgress(UUID tripId);
    TripResponse getTripById(UUID tripId);
    List<TripResponse> getTripsByDriver(UUID driverId);
    List<TripResponse> getTripsByFleetManager(UUID managerId);
    TripResponse getTripReport(UUID tripId);
}
