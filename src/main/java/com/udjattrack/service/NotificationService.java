package com.udjattrack.service;

import com.udjattrack.entity.Driver;
import com.udjattrack.entity.Trip;
import com.udjattrack.entity.User;

/**
 * NotificationService — dispatches in-app notifications to users.
 */
public interface NotificationService {

    void sendTripAssignedNotification(Driver driver, Trip trip);
    
    void sendTripCancelledNotification(Driver driver, Trip trip);
    
    void sendTripFinishedNotification(User manager, Trip trip);

    void sendAlertTriggeredNotification(User recipient, java.util.UUID alertId, String message);

    void sendIssueResolvedNotification(Driver driver, String issueType, java.util.UUID issueId);

    java.util.List<com.udjattrack.dto.response.NotificationResponse> getUserNotifications(java.util.UUID userId);

    void markAsRead(java.util.UUID notificationId, java.util.UUID userId);

    void markAllRead(java.util.UUID userId);
}
