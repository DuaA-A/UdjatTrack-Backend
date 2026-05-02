package com.udjattrack.entity;

import com.udjattrack.entity.enums.DriverState;
import com.udjattrack.entity.enums.TripProgressState;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TripState — a real-time snapshot of a trip's current status.
 * Updated continuously by the telemetry/mobile layer.
 * Tracks driver alertness, GPS position, and trip progress state.
 */
@Entity
@Table(name = "trip_states")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "state_id", updatable = false, nullable = false)
    private UUID stateId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false, unique = true)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "driver_state", length = 20)
    private DriverState driverState;

    @Enumerated(EnumType.STRING)
    @Column(name = "trip_progress_state", length = 20)
    private TripProgressState tripProgressState;

    @Column(name = "latitude", length = 50)
    private String latitude;

    @Column(name = "longitude", length = 50)
    private String longitude;

    @Column(name = "current_speed")
    private Double currentSpeed;

    @LastModifiedDate
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
}
