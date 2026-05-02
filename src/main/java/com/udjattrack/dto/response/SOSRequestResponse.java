package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.IssueStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class SOSRequestResponse {
    private UUID issueId;
    private UUID tripId;
    private IssueStatus status;
    private String location;
    private Boolean autoTriggered;
    private String description;
    private Map<String, Object> payload;
    private LocalDateTime triggeredAt;
}
