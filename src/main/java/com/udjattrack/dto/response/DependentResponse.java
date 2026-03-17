package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class DependentResponse {
    private UUID dependentId;
    private UUID driverId;
    private String name;
    private String phoneNumber;
    private String relation;
}
