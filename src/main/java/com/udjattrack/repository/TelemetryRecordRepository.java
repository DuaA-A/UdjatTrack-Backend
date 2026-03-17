package com.udjattrack.repository;

import com.udjattrack.entity.timeseries.TelemetryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecord, UUID> {

    List<TelemetryRecord> findAllByTripIdOrderByTimestampDesc(UUID tripId);

    Optional<TelemetryRecord> findTopByTripIdOrderByTimestampDesc(UUID tripId);

    @Query("""
            SELECT tr FROM TelemetryRecord tr
            WHERE tr.tripId = :tripId
            AND tr.timestamp BETWEEN :from AND :to
            ORDER BY tr.timestamp ASC
            """)
    List<TelemetryRecord> findByTripAndTimeRange(@Param("tripId") UUID tripId,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);
}
