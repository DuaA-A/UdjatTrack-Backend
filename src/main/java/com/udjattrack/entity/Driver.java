package com.udjattrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fleet_manager_id", nullable = false)
    @JsonIgnoreProperties({"drivers", "vehicles", "hibernateLazyInitializer", "handler"})
    private FleetManager fleetManager;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"driver", "hibernateLazyInitializer", "handler"})
    private List<Dependent> dependents = new ArrayList<>();

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    @JsonIgnoreProperties({"driver", "hibernateLazyInitializer", "handler"})
    private List<Trip> trips = new ArrayList<>();
}