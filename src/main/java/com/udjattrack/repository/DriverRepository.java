package com.udjattrack.repository;

import com.udjattrack.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findByEmailAndIsDeletedFalse(String email);

    List<Driver> findAllByFleetManagerUserIdAndIsDeletedFalse(UUID fleetManagerId);

    List<Driver> findAllByFleetManagerUserIdAndIdleTrue(UUID fleetManagerId);

    @Query("SELECT d FROM Driver d WHERE d.fleetManager.userId = :managerId AND d.isDeleted = false")
    List<Driver> findActiveDriversByManager(@Param("managerId") UUID managerId);

    boolean existsByLicenseNumberAndIsDeletedFalse(String licenseNumber);
}
