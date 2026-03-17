package com.udjattrack.repository;

import com.udjattrack.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByPlateNumberAndIsDeletedFalse(String plateNumber);

    List<Vehicle> findAllByFleetManagerUserIdAndIsDeletedFalse(UUID fleetManagerId);

    List<Vehicle> findAllByFleetManagerUserIdAndIdleTrue(UUID fleetManagerId);

    @Query("SELECT v FROM Vehicle v WHERE v.fleetManager.userId = :managerId AND v.isDeleted = false AND v.working = true")
    List<Vehicle> findWorkingVehiclesForManager(@Param("managerId") UUID managerId);

    boolean existsByPlateNumberAndIsDeletedFalse(String plateNumber);
}
