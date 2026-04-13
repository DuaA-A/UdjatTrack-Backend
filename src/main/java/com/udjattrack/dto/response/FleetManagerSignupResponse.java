package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class FleetManagerSignupResponse {
    private UUID fleetManagerId;
    private String status;
}
