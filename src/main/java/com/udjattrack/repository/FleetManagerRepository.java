package com.udjattrack.repository;

import com.udjattrack.entity.FleetManager;
import com.udjattrack.entity.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FleetManagerRepository extends JpaRepository<FleetManager, UUID> {

    Optional<FleetManager> findByEmail(String email);

    List<FleetManager> findAllByVerificationStatus(VerificationStatus status);

    List<FleetManager> findAllByIsDeletedFalse();

    @Query("SELECT fm FROM FleetManager fm WHERE fm.isDeleted = false AND fm.verificationStatus = :status")
    List<FleetManager> findActiveByVerificationStatus(VerificationStatus status);

    boolean existsByEmailAndIsDeletedFalse(String email);
}
