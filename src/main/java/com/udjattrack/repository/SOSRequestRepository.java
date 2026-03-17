package com.udjattrack.repository;

import com.udjattrack.entity.SOSRequest;
import com.udjattrack.entity.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SOSRequestRepository extends JpaRepository<SOSRequest, UUID> {

    List<SOSRequest> findAllByStatus(IssueStatus status);

    @Query("""
            SELECT s FROM SOSRequest s
            WHERE s.trip.driver.fleetManager.userId = :managerId
            ORDER BY s.triggeredAt DESC
            """)
    List<SOSRequest> findAllByFleetManager(@Param("managerId") UUID managerId);
}
