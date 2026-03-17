package com.udjattrack.service;

import com.udjattrack.entity.Driver;
import com.udjattrack.entity.Trip;
import com.udjattrack.entity.User;

/**
 * NotificationService — dispatches in-app notifications to users.
 */
public interface NotificationService {

    void sendTripAssignedNotification(Driver driver, Trip trip);

    void sendAlertTriggeredNotification(User fleetManager, java.util.UUID alertId);

    void markAllRead(java.util.UUID userId);
}
