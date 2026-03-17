package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.VerificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FleetManagerResponse {
    private UUID userId;
    private String name;
    private String email;
    private String companyName;
    private String subscriptionPlan;
    private VerificationStatus verificationStatus;
    private LocalDateTime createdAt;
}
