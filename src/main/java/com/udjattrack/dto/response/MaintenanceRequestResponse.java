package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.IssueStatus;
import com.udjattrack.entity.enums.MaintenanceType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class MaintenanceRequestResponse {
    private UUID issueId;
    private UUID tripId;
    private IssueStatus status;
    private MaintenanceType maintenanceType;
    private String description;
    private Map<String, Object> payload;
    private LocalDateTime triggeredAt;
}
