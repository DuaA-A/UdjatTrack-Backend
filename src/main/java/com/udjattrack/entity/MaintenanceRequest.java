package com.udjattrack.entity;

import com.udjattrack.entity.enums.MaintenanceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * MaintenanceRequest — raised when a vehicle has a mechanical/technical issue.
 * Extends IssueRequest and adds the specific maintenance type.
 */
@Entity
@Table(name = "maintenance_requests")
@DiscriminatorValue("MAINTENANCE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MaintenanceRequest extends IssueRequest {

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 40)
    private MaintenanceType maintenanceType;
}
