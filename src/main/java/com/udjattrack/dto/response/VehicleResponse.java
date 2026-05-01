package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class VehicleResponse {
    private UUID vehicleId;
    private String plateNumber;
    private String model;
    private Integer manufactureYear;
    private String licenseNumber;
    private Boolean idle;
    private Boolean working;
    private UUID fleetManagerId;
    private LocalDateTime createdAt;
}
