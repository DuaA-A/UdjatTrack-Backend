package com.udjattrack.repository;

import com.udjattrack.entity.TripState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripStateRepository extends JpaRepository<TripState, UUID> {

    Optional<TripState> findByTripTripId(UUID tripId);
}
