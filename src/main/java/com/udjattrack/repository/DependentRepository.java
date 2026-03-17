package com.udjattrack.repository;

import com.udjattrack.entity.Dependent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DependentRepository extends JpaRepository<Dependent, UUID> {

    List<Dependent> findAllByDriverUserId(UUID driverId);
}
