package com.udjattrack.service;

import com.udjattrack.dto.request.TelemetryBatchRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.TelemetryRecordResponse;
import com.udjattrack.dto.response.TripStateResponse;

import java.util.List;
import java.util.UUID;
public interface TelemetryService {

    TelemetryRecordResponse ingestTelemetry(UUID tripId, TelemetryRequest request);

    void processTelemetryBatch(UUID tripId, TelemetryBatchRequest batchRequest);   

    TripStateResponse updateTripState(UUID tripId, String driverState, String progressState);

    void updateDriverStatus(UUID driverId, String newState);

    void updateVehicleState(UUID vehicleId, boolean isWorking);

    List<TripStateResponse> getLiveFleetStatus(UUID fleetManagerId);

    List<TelemetryRecordResponse> getTelemetryByTrip(UUID tripId);
}
