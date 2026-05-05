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
    private final com.udjattrack.service.NotificationService notificationService;

    @Override
    public SOSRequestResponse sendManualSOS(CreateSOSRequest request) {
        Trip trip = findTrip(request.tripId());
        SOSRequest sos = SOSRequest.builder()
                .trip(trip)
                .location(request.location())
                .autoTriggered(false)
                .status(IssueStatus.OPEN)
                .payload(request.payload())
                .triggeredAt(java.time.LocalDateTime.now())
                .build();
        SOSRequest saved = sosRequestRepository.saveAndFlush(sos);
        
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
                .triggeredAt(java.time.LocalDateTime.now())
                .build();
        SOSRequest saved = sosRequestRepository.saveAndFlush(sos);
        
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
                .triggeredAt(java.time.LocalDateTime.now())
                .build();
        MaintenanceRequest saved = maintenanceRequestRepository.saveAndFlush(maintenance);
        
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
                .triggeredAt(java.time.LocalDateTime.now())
                .build();
        Incident saved = incidentRepository.saveAndFlush(incident);
        
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

    @Override
    @Transactional(readOnly = true)
    public List<IncidentResponse> getAllIncidents() {
        return incidentRepository.findAll()
                .stream().map(this::toIncidentResponse).collect(Collectors.toList());
    }

    @Override
    public void resolveIssue(UUID issueId, String resolutionNote) {
        // Try SOS
        sosRequestRepository.findById(issueId).ifPresent(sos -> {
            sos.setStatus(IssueStatus.RESOLVED);
            if (resolutionNote != null) {
                sos.getPayload().put("resolutionNote", resolutionNote);
            }
            sosRequestRepository.save(sos);
            notificationService.sendIssueResolvedNotification(sos.getTrip().getDriver(), "SOS", issueId);
        });

        // Try Maintenance
        maintenanceRequestRepository.findById(issueId).ifPresent(m -> {
            m.setStatus(IssueStatus.RESOLVED);
            if (resolutionNote != null) {
                m.getPayload().put("resolutionNote", resolutionNote);
            }
            maintenanceRequestRepository.save(m);
            notificationService.sendIssueResolvedNotification(m.getTrip().getDriver(), "Maintenance", issueId);
        });
        
        // Try Incident
        incidentRepository.findById(issueId).ifPresent(i -> {
            i.setStatus(IssueStatus.RESOLVED);
            if (resolutionNote != null) {
                i.getPayload().put("resolutionNote", resolutionNote);
            }
            incidentRepository.save(i);
            notificationService.sendIssueResolvedNotification(i.getTrip().getDriver(), "Incident", issueId);
        });
    }

    // ===== Private helpers =====

    private Trip findTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
    }

    private void notifyDependentsOnSOS(Trip trip, String location) {
        String driverName = trip.getDriver().getName();
        String loc = location != null ? location : "Unknown location";
        
        // Notify Fleet Manager via Email
        if (trip.getDriver() != null && trip.getDriver().getFleetManager() != null) {
            FleetManager manager = trip.getDriver().getFleetManager();
            emailService.sendEmergencyAlert(manager.getEmail(), driverName, loc);
        }

        // Notify dependents (placeholder logic remains but manager is the primary recipient now)
        dependentRepository.findAllByDriverUserId(trip.getDriver().getUserId()).forEach(dep ->
                log.info("SOS notification triggered for dependent: {} [Phone: {}]", dep.getName(), dep.getPhoneNumber())
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
