package com.udjattrack.service;

import com.udjattrack.dto.request.TelemetryBatchRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.TelemetryRecordResponse;
import com.udjattrack.dto.response.TripStateResponse;

import java.util.List;
import java.util.UUID;

/**
 * Telemetry service — ingests real-time and batch IoT data from driver devices.
 * Updates trip state and driver status, and triggers alert evaluation.
 */
public interface TelemetryService {

    TelemetryRecordResponse ingestTelemetry(TelemetryRequest request);

    void processTelemetryBatch(TelemetryBatchRequest batchRequest);   // @Async

    TripStateResponse updateTripState(UUID tripId, String driverState, String progressState);

    void updateDriverStatus(UUID driverId, String newState);

    void updateVehicleState(UUID vehicleId, boolean isWorking);

    List<TripStateResponse> getLiveFleetStatus(UUID fleetManagerId);

    List<TelemetryRecordResponse> getTelemetryByTrip(UUID tripId);
}
