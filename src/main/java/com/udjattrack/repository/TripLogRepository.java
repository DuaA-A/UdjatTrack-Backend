package com.udjattrack.repository;

import com.udjattrack.entity.TripLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripLogRepository extends JpaRepository<TripLog, UUID> {

    Optional<TripLog> findByTripTripId(UUID tripId);
}
