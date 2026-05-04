package com.udjattrack.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * GeneralNotification — used for simple text notifications that don't
 * require specialized fields (e.g., Trip Cancelled, Maintenance Resolved).
 */
@Entity
@Table(name = "general_notifications")
@DiscriminatorValue("GENERAL")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class GeneralNotification extends Notification {
    // No extra fields needed, uses title and message from base Notification
}
