package com.udjattrack.repository;

import com.udjattrack.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findAllByTripTripIdOrderByTriggeredAtDesc(UUID tripId);

    @Query("""
            SELECT i FROM Incident i
            WHERE i.trip.driver.fleetManager.userId = :managerId
            ORDER BY i.triggeredAt DESC
            """)
    List<Incident> findAllByFleetManager(@Param("managerId") UUID managerId);
}
