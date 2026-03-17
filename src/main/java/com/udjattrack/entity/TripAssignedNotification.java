package com.udjattrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TripAssignedNotification — sent to driver when assigned to a new trip.
 */
@Entity
@Table(name = "trip_assigned_notifications")
@DiscriminatorValue("TRIP_ASSIGNED")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TripAssignedNotification extends Notification {

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;
}
