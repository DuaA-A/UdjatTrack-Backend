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
    @Query("SELECT a FROM Alert a WHERE a.trip.driver.fleetManager.userId = :fleetManagerId ORDER BY a.timestamp DESC")
    List<Alert> findAllByFleetManager(@Param("fleetManagerId") UUID fleetManagerId);

    @Query("""
            SELECT a FROM Alert a
            WHERE a.trip.driver.fleetManager.userId = :managerId
            AND a.acknowledged = true
            ORDER BY a.timestamp DESC
            """)
    List<Alert> findAcknowledgedAlertsForFleetManager(@Param("managerId") UUID managerId);

    @Query("SELECT a FROM Alert a WHERE a.trip.driver.fleetManager.userId = :fleetManagerId AND a.timestamp >= :startDate ORDER BY a.timestamp ASC")
    List<Alert> findAllByFleetManagerAndTimestampAfter(@Param("fleetManagerId") UUID fleetManagerId, @Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT a FROM Alert a WHERE a.trip.driver.fleetManager.userId = :fleetManagerId AND a.timestamp >= :startOfDay AND a.timestamp <= :endOfDay")
    List<Alert> findAllByFleetManagerForToday(@Param("fleetManagerId") UUID fleetManagerId, @Param("startOfDay") java.time.LocalDateTime startOfDay, @Param("endOfDay") java.time.LocalDateTime endOfDay);
}
