package com.udjattrack.repository;

import com.udjattrack.entity.Alert;
import com.udjattrack.entity.enums.AlertType;
import com.udjattrack.entity.enums.SeverityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findAllByTripTripIdOrderByTimestampDesc(UUID tripId);

    List<Alert> findAllByAcknowledgedFalse();

    List<Alert> findAllByAlertTypeAndAcknowledgedFalse(AlertType alertType);

    @Query("""
            SELECT a FROM Alert a
            WHERE a.trip.driver.fleetManager.userId = :managerId
            AND a.acknowledged = false
            ORDER BY a.timestamp DESC
            """)
    List<Alert> findUnacknowledgedAlertsForFleetManager(@Param("managerId") UUID managerId);

    @Query("""
            SELECT a FROM Alert a
            WHERE a.trip.driver.fleetManager.userId = :managerId
            AND a.severity = :severity
            ORDER BY a.timestamp DESC
            """)
    List<Alert> findBySeverityForFleetManager(@Param("managerId") UUID managerId,
                                               @Param("severity") SeverityLevel severity);
}
