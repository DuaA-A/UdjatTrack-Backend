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
 * Inherits common fields (trip, payload, reportedAt) from IssueRequest.
 */
@Entity
@Table(name = "incidents")
@DiscriminatorValue("INCIDENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Incident extends IssueRequest {

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 15)
    private SeverityLevel severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 40)
    private IncidentType type;

    @Column(name = "location", length = 255)
    private String location;
}
