package com.udjattrack.service;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.*;

import java.util.List;
import java.util.UUID;

/**
 * Emergency service — handles SOS requests, auto-triggered SOS,
 * incident reporting, and maintenance requests.
 */
public interface EmergencyService {

    SOSRequestResponse sendManualSOS(CreateSOSRequest request);
    SOSRequestResponse sendAutoSOS(CreateSOSRequest request);
    void notifyEmergencyService(UUID sosRequestId);
    MaintenanceRequestResponse reportMaintenanceRequest(CreateMaintenanceRequest request);
    IncidentResponse createIncident(CreateIncidentRequest request);
    List<SOSRequestResponse> getSOSRequestsByFleetManager(UUID managerId);
    List<MaintenanceRequestResponse> getMaintenanceRequestsByFleetManager(UUID managerId);
    List<IncidentResponse> getIncidentsByFleetManager(UUID managerId);
    void resolveIssue(UUID issueId, String resolutionNote);
}
