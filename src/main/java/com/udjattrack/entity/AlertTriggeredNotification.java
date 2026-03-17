package com.udjattrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * AlertTriggeredNotification — dispatched to FleetManager when an alert fires.
 */
@Entity
@Table(name = "alert_triggered_notifications")
@DiscriminatorValue("ALERT_TRIGGERED")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AlertTriggeredNotification extends Notification {

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;
}
