package com.udjattrack.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
@Entity
@Table(name = "dependents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dependent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "dependent_id", updatable = false, nullable = false)
    private UUID dependentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "relation", length = 50)
    private String relation;
}
