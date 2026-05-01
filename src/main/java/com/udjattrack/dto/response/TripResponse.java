package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.TripStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TripResponse {
    // --- Persistent DB fields ---
    private UUID tripId;
    private UUID driverId;
    private String driverName;
    private UUID vehicleId;
    private String vehiclePlate;
    private String source;
    private String destination;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private TripStatus status;
    private LocalDateTime createdAt;
    private TripLogResponse tripLog;

    // --- Calculated / Transient fields (populated by Service layer, never stored) ---
    /** Derived from source + " → " + destination */
    private String title;
    /** Derived from scheduledEndTime - scheduledStartTime in hours */
    private Double expectedDurationHours;
    /** Estimated arrival based on actual start + expected duration */
    private LocalDateTime estimatedArrivalTime;
    /** Progress percentage: 0-100, based on elapsed time vs total expected duration */
    private Integer progressPct;
    /** Total distance in km — provided by mobile telemetry, stored in TripLog if available */
    private Double totalDistanceKm;
}
