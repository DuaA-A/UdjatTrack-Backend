package com.udjattrack.repository;

import com.udjattrack.entity.Trip;
import com.udjattrack.entity.enums.TripProgressState;
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

    List<Trip> findAllByDriverUserIdAndTripStateInOrderByCreatedAtDesc(UUID driverId, List<TripProgressState> states);

    List<Trip> findAllByVehicleVehicleIdOrderByCreatedAtDesc(UUID vehicleId);

    List<Trip> findAllByTripState(TripProgressState state);

    @Query("""
            SELECT t FROM Trip t
            WHERE t.driver.fleetManager.userId = :managerId
            ORDER BY t.createdAt DESC
            """)
    List<Trip> findAllByFleetManager(@Param("managerId") UUID managerId);

    @Query("""
            SELECT t FROM Trip t
            WHERE t.driver.userId = :driverId
            AND t.tripState NOT IN ('COMPLETED', 'CANCELLED')
            """)
    Optional<Trip> findActiveTrip(@Param("driverId") UUID driverId);
}
