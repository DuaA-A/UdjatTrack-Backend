package com.udjattrack.repository;

import com.udjattrack.entity.Trip;
import com.udjattrack.entity.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findAllByDriverUserIdOrderByCreatedAtDesc(UUID driverId);

    List<Trip> findAllByDriverUserIdAndStatusInOrderByCreatedAtDesc(UUID driverId, List<TripStatus> statuses);

    List<Trip> findAllByVehicleVehicleIdOrderByCreatedAtDesc(UUID vehicleId);

    List<Trip> findAllByStatus(TripStatus status);

    @Query("""
            SELECT t FROM Trip t
            WHERE t.driver.fleetManager.userId = :managerId
            ORDER BY t.createdAt DESC
            """)
    List<Trip> findAllByFleetManager(@Param("managerId") UUID managerId);

    @Query("""
            SELECT t FROM Trip t
            WHERE t.driver.userId = :driverId
            AND t.status NOT IN (com.udjattrack.entity.enums.TripStatus.FINISHED, com.udjattrack.entity.enums.TripStatus.CANCELLED)
            """)
    Optional<Trip> findActiveTrip(@Param("driverId") UUID driverId);

    @Query("""
            SELECT t FROM Trip t
            WHERE t.driver.userId = :driverId
            AND t.status != com.udjattrack.entity.enums.TripStatus.CANCELLED
            AND t.scheduledStartTime < :endTime
            AND t.scheduledEndTime > :startTime
            """)
    List<Trip> findConflictingTripsForDriver(@Param("driverId") UUID driverId, 
                                             @Param("startTime") java.time.LocalDateTime startTime, 
                                             @Param("endTime") java.time.LocalDateTime endTime);

    @Query("""
            SELECT t FROM Trip t
            WHERE t.vehicle.vehicleId = :vehicleId
            AND t.status != com.udjattrack.entity.enums.TripStatus.CANCELLED
            AND t.scheduledStartTime < :endTime
            AND t.scheduledEndTime > :startTime
            """)
    List<Trip> findConflictingTripsForVehicle(@Param("vehicleId") UUID vehicleId, 
                                              @Param("startTime") java.time.LocalDateTime startTime, 
                                              @Param("endTime") java.time.LocalDateTime endTime);
}
