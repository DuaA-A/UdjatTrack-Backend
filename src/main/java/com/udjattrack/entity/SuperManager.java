package com.udjattrack.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "super_managers")
@DiscriminatorValue("SUPER_MANAGER")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class SuperManager extends User {
}
