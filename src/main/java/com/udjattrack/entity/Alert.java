package com.udjattrack.entity;

import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Alert — raised when the system detects a critical event during a trip.
 * Linked directly to the active Trip (not TripLog) to support real-time alerting.
 * Uses alertableType/alertableId as a polymorphic reference to the source issue
 * (e.g., Incident, SOSRequest, MaintenanceRequest).
 */
@Entity
@Table(name = "alerts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "alert_id", updatable = false, nullable = false)
    private UUID alertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    // Polymorphic reference to the source entity that triggered this alert
    // e.g. 'Incident', 'SOSRequest', 'MaintenanceRequest'
    @Column(name = "alertable_type", nullable = false, length = 50)
    private String alertableType;

    @Column(name = "alertable_id", nullable = false)
    private UUID alertableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 15)
    private SeverityLevel severity;

    @Column(name = "acknowledged")
    private Boolean acknowledged = false;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @CreatedDate
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}