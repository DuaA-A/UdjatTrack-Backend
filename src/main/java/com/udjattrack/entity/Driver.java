package com.udjattrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Driver — operates vehicles and takes trips.
 * Belongs to a FleetManager. Can have dependents registered.
 */
@Entity
@Table(name = "drivers")
@DiscriminatorValue("DRIVER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Driver extends User {

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "is_idle")
    private Boolean idle = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fleet_manager_id", nullable = false)
    private FleetManager fleetManager;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dependent> dependents = new ArrayList<>();

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    private List<Trip> trips = new ArrayList<>();
}
