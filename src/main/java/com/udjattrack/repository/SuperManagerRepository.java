package com.udjattrack.repository;
import com.udjattrack.entity.SuperManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface SuperManagerRepository extends JpaRepository<SuperManager, UUID> {
    Optional<SuperManager> findByEmail(String email);
    boolean existsByEmail(String email);
}
