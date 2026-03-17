package com.udjattrack.repository;

import com.udjattrack.entity.MaintenanceRequest;
import com.udjattrack.entity.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, UUID> {

    List<MaintenanceRequest> findAllByStatus(IssueStatus status);

    @Query("""
            SELECT m FROM MaintenanceRequest m
            WHERE m.trip.driver.fleetManager.userId = :managerId
            ORDER BY m.triggeredAt DESC
            """)
    List<MaintenanceRequest> findAllByFleetManager(@Param("managerId") UUID managerId);
}
