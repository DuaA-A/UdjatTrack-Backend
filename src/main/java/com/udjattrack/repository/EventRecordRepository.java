package com.udjattrack.repository;

import com.udjattrack.entity.EventRecord;
import com.udjattrack.entity.enums.SeverityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRecordRepository extends JpaRepository<EventRecord, UUID> {

    List<EventRecord> findAllByTripTripIdOrderByTimestampDesc(UUID tripId);

    List<EventRecord> findAllByTripTripIdAndSeverity(UUID tripId, SeverityLevel severity);

    List<EventRecord> findAllByTripTripIdAndEventTypeOrderByTimestampDesc(UUID tripId, String eventType);
}
