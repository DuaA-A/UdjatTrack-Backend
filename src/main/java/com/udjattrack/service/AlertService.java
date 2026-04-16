package com.udjattrack.service;

import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.AlertResponse;

import java.util.List;
import java.util.UUID;

/**
 * Alert service — handles alert creation, acknowledgment, and querying.
 * Also evaluates telemetry events for alert conditions.
 */
public interface AlertService {

    AlertResponse createAlert(CreateAlertRequest request);
    AlertResponse getAlertById(UUID alertId);
    AlertResponse acknowledgeAlert(UUID alertId);
    List<AlertResponse> getUnacknowledgedAlerts(UUID fleetManagerId);
    List<AlertResponse> getAlertsByTripLog(UUID tripLogId);
    void evaluateEvent(TelemetryRequest telemetryRequest);
    void notifyFleetManager(UUID alertId);
}
