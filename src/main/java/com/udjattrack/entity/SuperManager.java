package com.udjattrack.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * SuperManager — top-level administrator who manages the entire platform.
 * Can verify, update, and delete FleetManagers.
 */
@Entity
@Table(name = "super_managers")
@DiscriminatorValue("SUPER_MANAGER")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SuperManager extends User {
    // SuperManager has no additional fields beyond User.
    // Future: add platform-level settings or billing here.
}
