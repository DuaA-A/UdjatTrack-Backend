package com.udjattrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "alert_id", updatable = false, nullable = false)
    private UUID alertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnoreProperties({"driver", "vehicle", "tripLog", "tripState", "hibernateLazyInitializer", "handler"})
    private Trip trip;

    @Column(name = "alertable_type", nullable = false, length = 50)
    private String alertableType;

    @Column(name = "alertable_id", nullable = false)
    private UUID alertableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 15)
    private SeverityLevel severity;

    @Column(name = "acknowledged")
    private Boolean acknowledged = false;

    @Column(name = "acked_at")
    private LocalDateTime ackedAt;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @CreatedDate
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}