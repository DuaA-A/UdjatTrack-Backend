package com.udjattrack.repository.timeseries;

import com.udjattrack.entity.timeseries.EventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRecordTimeSeriesRepository extends JpaRepository<EventRecord, UUID> {
    List<EventRecord> findAllByTripIdOrderByTimestampDesc(UUID tripId);
}
