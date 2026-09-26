package com.udjattrack.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "general_notifications")
@DiscriminatorValue("GENERAL")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class GeneralNotification extends Notification {
}
