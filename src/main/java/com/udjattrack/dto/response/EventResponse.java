package com.udjattrack.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class EventResponse {
    private UUID eventId;
    private boolean alertCreated;
}
