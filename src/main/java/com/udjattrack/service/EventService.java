package com.udjattrack.service;

import com.udjattrack.dto.request.EventRequest;
import com.udjattrack.dto.request.OfflineSyncRequest;
import com.udjattrack.dto.response.EventResponse;
import java.util.UUID;

public interface EventService {
    EventResponse reportEvent(UUID tripId, EventRequest request);
    void syncOfflineData(OfflineSyncRequest request);
    // Add other methods if needed for retrieving events
}
