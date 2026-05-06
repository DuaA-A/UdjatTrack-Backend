package com.udjattrack.entity.timeseries;

import com.udjattrack.entity.enums.DriverState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * TelemetryRecord — a timestamped telemetry data point from the vehicle/driver.
 * Lives in the 'udjattrack_ts' database (time-series optimized schema).
 */
@Entity
@Table(name = "telemetry_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "telemetry_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "driver_state", length = 20)
    private DriverState driverState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "json")
    private Map<String, Object> details;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}
