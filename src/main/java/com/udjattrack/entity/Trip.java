package com.udjattrack.entity;

import com.udjattrack.entity.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Trip — the core domain object representing a driver's route/mission.
 * A trip is assigned a driver and a vehicle, tracks its lifecycle via TripStatus.
 */
@Entity
@Table(name = "trips")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "trip_id", updatable = false, nullable = false)
    private UUID tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "source", nullable = false, length = 255)
    private String source;

    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_state", nullable = false, length = 20)
    private TripStatus status = TripStatus.PLANNED;

    @OneToOne(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private TripLog tripLog;

    @Builder.Default
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Incident> incidents = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventRecord> eventRecords = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}