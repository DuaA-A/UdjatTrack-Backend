package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DriverResponse {
    private UUID userId;
    private String name;
    private String email;
    private String licenseNumber;
    private String phoneNumber;
    private Boolean idle;
    private UUID fleetManagerId;
    private LocalDateTime createdAt;
}
