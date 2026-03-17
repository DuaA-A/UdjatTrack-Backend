package com.udjattrack.entity;

import com.udjattrack.entity.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * FleetManager — manages a fleet of vehicles and drivers under a company.
 * Must be verified by SuperManager before accessing the platform.
 */
@Entity
@Table(name = "fleet_managers")
@DiscriminatorValue("FLEET_MANAGER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FleetManager extends User {

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "subscription_plan", length = 100)
    private String subscriptionPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @OneToMany(mappedBy = "fleetManager", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Driver> drivers = new ArrayList<>();

    @OneToMany(mappedBy = "fleetManager", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vehicle> vehicles = new ArrayList<>();
}
