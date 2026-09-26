package com.udjattrack.service;

import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.AlertResponse;
import com.udjattrack.dto.response.AlertSummaryResponse;
import com.udjattrack.dto.response.AlertDailyCountResponse;

import java.util.List;
import java.util.UUID;

public interface AlertService {

    AlertResponse createAlert(CreateAlertRequest request);
    AlertResponse getAlertById(UUID alertId);
    AlertResponse acknowledgeAlert(UUID alertId);
    List<AlertResponse> getUnacknowledgedAlerts(UUID fleetManagerId);
    List<AlertResponse> getAcknowledgedAlerts(UUID fleetManagerId);
    List<AlertResponse> getAlertsByTrip(UUID tripId);
    List<AlertResponse> getAllAlertsByFleetManager(UUID fleetManagerId);
    AlertSummaryResponse getAlertSummary(UUID fleetManagerId);
    List<AlertDailyCountResponse> getAlertDailyCounts(UUID fleetManagerId, int days);
    void evaluateEvent(TelemetryRequest telemetryRequest);
    void notifyFleetManager(UUID alertId);
}
