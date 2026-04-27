package com.udjattrack.service;

import com.udjattrack.dto.request.EventRequest;
import com.udjattrack.dto.request.OfflineSyncRequest;
import com.udjattrack.dto.response.EventResponse;
import java.util.UUID;

public interface EventService {
    EventResponse reportEvent(UUID tripId, EventRequest request);
    java.util.List<EventResponse> getEventsByTrip(UUID tripId);
    EventResponse getEventById(UUID eventId);
    java.util.List<EventResponse> getAllEvents(UUID tripId, String eventType);
    void syncOfflineData(OfflineSyncRequest request);
}
