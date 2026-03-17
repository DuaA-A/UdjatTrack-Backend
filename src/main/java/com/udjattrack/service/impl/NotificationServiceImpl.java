package com.udjattrack.service.impl;

import com.udjattrack.entity.*;
import com.udjattrack.entity.enums.NotificationType;
import com.udjattrack.repository.NotificationRepository;
import com.udjattrack.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void sendTripAssignedNotification(Driver driver, Trip trip) {
        TripAssignedNotification notification = TripAssignedNotification.builder()
                .recipient(driver)
                .title("New Trip Assigned")
                .message("You have been assigned a trip from " + trip.getSource()
                        + " to " + trip.getDestination())
                .type(NotificationType.TRIP_ASSIGNED)
                .isRead(false)
                .tripId(trip.getTripId())
                .scheduledStartTime(trip.getScheduledStartTime())
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public void sendAlertTriggeredNotification(User fleetManager, UUID alertId) {
        AlertTriggeredNotification notification = AlertTriggeredNotification.builder()
                .recipient(fleetManager)
                .title("Alert Triggered")
                .message("A new alert has been triggered for your fleet. Please review.")
                .type(NotificationType.ALERT_TRIGGERED)
                .isRead(false)
                .alertId(alertId)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
