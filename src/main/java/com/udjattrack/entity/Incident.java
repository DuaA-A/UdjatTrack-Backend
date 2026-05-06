package com.udjattrack.entity;

import com.udjattrack.entity.enums.IncidentType;
import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Incident — reported during or after a trip by the driver or auto-detected.
 * Inherits common fields (trip, payload, triggeredAt) from IssueRequest.
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
