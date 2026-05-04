package com.udjattrack.service.impl;

import com.udjattrack.dto.response.NotificationResponse;
import com.udjattrack.entity.*;
import com.udjattrack.entity.enums.NotificationType;
import com.udjattrack.repository.NotificationRepository;
import com.udjattrack.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public void sendTripCancelledNotification(Driver driver, Trip trip) {
        Notification notification = GeneralNotification.builder()
                .recipient(driver)
                .title("Trip Cancelled")
                .message("Your trip from " + trip.getSource() + " to " + trip.getDestination() + " has been cancelled.")
                .type(NotificationType.TRIP_CANCELLED)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public void sendTripFinishedNotification(User manager, Trip trip) {
        Notification notification = GeneralNotification.builder()
                .recipient(manager)
                .title("Trip Finished")
                .message("Driver " + trip.getDriver().getName() + " has finished the trip to " + trip.getDestination())
                .type(NotificationType.TRIP_FINISHED)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public void sendAlertTriggeredNotification(User recipient, UUID alertId, String message) {
        AlertTriggeredNotification notification = AlertTriggeredNotification.builder()
                .recipient(recipient)
                .title("Safety Alert")
                .message(message != null ? message : "A safety alert has been triggered.")
                .type(NotificationType.ALERT_TRIGGERED)
                .isRead(false)
                .alertId(alertId)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public void sendIssueResolvedNotification(Driver driver, String issueType, UUID issueId) {
        Notification notification = GeneralNotification.builder()
                .recipient(driver)
                .title(issueType + " Resolved")
                .message("Your " + issueType.toLowerCase() + " request has been marked as resolved.")
                .type(NotificationType.GENERAL)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId) {
        return notificationRepository.findAllByRecipientUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient().getUserId().equals(userId)) {
                n.setIsRead(true);
            }
        });
    }

    @Override
    public void markAllRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .type(notification.getType())
                .createdAt(notification.getCreatedAt())
                .details(notification.getDetails())
                .build();
    }
}
