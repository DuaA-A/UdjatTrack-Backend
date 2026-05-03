package com.udjattrack.service.impl;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.*;
import com.udjattrack.entity.*;
import com.udjattrack.entity.enums.IssueStatus;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.*;
import com.udjattrack.service.EmailService;
import com.udjattrack.service.EmergencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmergencyServiceImpl implements EmergencyService {

    private final SOSRequestRepository sosRequestRepository;
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final IncidentRepository incidentRepository;
    private final TripRepository tripRepository;
    private final DependentRepository dependentRepository;
    private final EmailService emailService;
    private final com.udjattrack.service.AlertService alertService;

    @Override
    public SOSRequestResponse sendManualSOS(CreateSOSRequest request) {
        Trip trip = findTrip(request.tripId());
        SOSRequest sos = SOSRequest.builder()
                .trip(trip)
                .location(request.location())
                .autoTriggered(false)
                .status(IssueStatus.OPEN)
                .payload(request.payload())
                .build();
        SOSRequest saved = sosRequestRepository.save(sos);
        
        // Trigger WebSocket Alert
        alertService.createAlert(new CreateAlertRequest(
                request.tripId(), com.udjattrack.entity.enums.AlertType.SOS_REQ, 
                com.udjattrack.entity.enums.SeverityLevel.CRITICAL, 
                "SOSRequest", saved.getIssueId(), "CRITICAL: SOS Triggered"
        ));
        
        notifyDependentsOnSOS(trip, request.location());
        log.warn("Manual SOS raised for trip: {}", request.tripId());
        return toSOSResponse(saved);
    }

    @Override
    public SOSRequestResponse sendAutoSOS(CreateSOSRequest request) {
        Trip trip = findTrip(request.tripId());
        SOSRequest sos = SOSRequest.builder()
                .trip(trip)
                .location(request.location())
                .autoTriggered(true)
                .status(IssueStatus.OPEN)
                .payload(request.payload())
                .build();
        SOSRequest saved = sosRequestRepository.save(sos);
        
        // Trigger WebSocket Alert
        alertService.createAlert(new CreateAlertRequest(
                request.tripId(), com.udjattrack.entity.enums.AlertType.SOS_REQ, 
                com.udjattrack.entity.enums.SeverityLevel.CRITICAL, 
                "SOSRequest", saved.getIssueId(), "SYSTEM AUTO-DETECT: SOS Triggered"
        ));
        
        notifyDependentsOnSOS(trip, request.location());
        log.warn("Auto SOS raised for trip: {}", request.tripId());
        return toSOSResponse(saved);
    }

    @Override
    public void notifyEmergencyService(UUID sosRequestId) {
        SOSRequest sos = sosRequestRepository.findById(sosRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("SOSRequest", "id", sosRequestId));
        log.warn("Emergency services notified for SOS: {} at location: {}",
                sosRequestId, sos.getLocation());
        // Future: integrate with 3rd-party emergency dispatch API
    }

    @Override
    public MaintenanceRequestResponse reportMaintenanceRequest(CreateMaintenanceRequest request) {
        Trip trip = findTrip(request.tripId());
        MaintenanceRequest maintenance = MaintenanceRequest.builder()
                .trip(trip)
                .maintenanceType(request.maintenanceType())
                .status(IssueStatus.OPEN)
                .payload(request.payload())
                .build();
        MaintenanceRequest saved = maintenanceRequestRepository.save(maintenance);
        
        // Trigger WebSocket Alert
        alertService.createAlert(new CreateAlertRequest(
                request.tripId(), com.udjattrack.entity.enums.AlertType.MAINTENANCE, 
                com.udjattrack.entity.enums.SeverityLevel.MEDIUM, 
                "MaintenanceRequest", saved.getIssueId(), "Maintenance Required: " + request.maintenanceType()
        ));
        
        return toMaintenanceResponse(saved);
    }

    @Override
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        Trip trip = findTrip(request.tripId());
        Incident incident = Incident.builder()
                .trip(trip)
                .type(request.type())
                .severity(request.severity())
                .location(request.location())
                .status(IssueStatus.OPEN)
                .payload(request.payload())
                .build();
        Incident saved = incidentRepository.save(incident);
        
        // Trigger WebSocket Alert
        alertService.createAlert(new CreateAlertRequest(
                request.tripId(), com.udjattrack.entity.enums.AlertType.INCIDENT, 
                request.severity(), 
                "Incident", saved.getIssueId(), "Incident Reported: " + request.type()
        ));
        
        return toIncidentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SOSRequestResponse> getSOSRequestsByFleetManager(UUID managerId) {
        return sosRequestRepository.findAllByFleetManager(managerId)
                .stream().map(this::toSOSResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceRequestResponse> getMaintenanceRequestsByFleetManager(UUID managerId) {
        return maintenanceRequestRepository.findAllByFleetManager(managerId)
                .stream().map(this::toMaintenanceResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidentsByFleetManager(UUID managerId) {
        return incidentRepository.findAllByFleetManager(managerId)
                .stream().map(this::toIncidentResponse).collect(Collectors.toList());
    }

    // ===== Private helpers =====

    private Trip findTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
    }

    private void notifyDependentsOnSOS(Trip trip, String location) {
        String driverName = trip.getDriver().getName();
        String loc = location != null ? location : "Unknown location";
        dependentRepository.findAllByDriverUserId(trip.getDriver().getUserId()).forEach(dep ->
                emailService.sendEmergencyAlert(dep.getPhoneNumber() + "@placeholder.com",
                        driverName, loc)
        );
    }

    private SOSRequestResponse toSOSResponse(SOSRequest s) {
        return SOSRequestResponse.builder()
                .issueId(s.getIssueId()).tripId(s.getTrip().getTripId())
                .status(s.getStatus())                .location(s.getLocation())
                .autoTriggered(s.getAutoTriggered())
                .payload(s.getPayload())
                .triggeredAt(s.getTriggeredAt())
                .build();
    }

    private MaintenanceRequestResponse toMaintenanceResponse(MaintenanceRequest m) {
        return MaintenanceRequestResponse.builder()
                .issueId(m.getIssueId()).tripId(m.getTrip().getTripId())
                .status(m.getStatus())
                .maintenanceType(m.getMaintenanceType())
                .payload(m.getPayload())
                .triggeredAt(m.getTriggeredAt())
                .build();
    }

    private IncidentResponse toIncidentResponse(Incident i) {
        return IncidentResponse.builder()
                .incidentId(i.getIssueId()).tripId(i.getTrip().getTripId())
                .severity(i.getSeverity())
                .type(i.getType())
                .location(i.getLocation())
                .payload(i.getPayload())
                .triggeredAt(i.getTriggeredAt())
                .build();
    }
}
