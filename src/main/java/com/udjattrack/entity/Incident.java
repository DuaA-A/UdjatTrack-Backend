package com.udjattrack.entity;

import com.udjattrack.entity.enums.IncidentType;
import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Incident — reported during or after a trip by the driver or auto-detected.
 * Supports a flexible JSON payload for incident-specific metadata.
 */
@Entity
@Table(name = "incidents")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "incident_id", updatable = false, nullable = false)
    private UUID incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 15)
    private SeverityLevel severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 40)
    private IncidentType type;

    @Column(name = "location", length = 255)
    private String location;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "json")
    private Map<String, Object> payload;

    @CreatedDate
    @Column(name = "reported_at", nullable = false, updatable = false)
    private LocalDateTime reportedAt;
}
