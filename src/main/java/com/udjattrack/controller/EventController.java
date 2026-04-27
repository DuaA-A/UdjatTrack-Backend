package com.udjattrack.controller;

import com.udjattrack.dto.request.EventRequest;
import com.udjattrack.dto.request.OfflineSyncRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.EventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Events & Sync", description = "Safety events and offline data synchronization")
public class EventController {

    private final com.udjattrack.service.EventService eventService;

    @PostMapping("/trips/{tripId}/events")
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Sent instantly upon critical condition detection")
    public ResponseEntity<ApiResponse<EventResponse>> reportEvent(
            @PathVariable UUID tripId,
            @Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.reportEvent(tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Event recorded", response));
    }

    @GetMapping("/events")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "List events with filters")
    public ResponseEntity<ApiResponse<List<EventResponse>>> listEvents(
            @RequestParam(required = false) UUID tripId,
            @RequestParam(required = false) String eventType) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getAllEvents(tripId, eventType)));
    }

    @GetMapping("/events/{id}")
    @PreAuthorize("hasAnyRole('ROLE_FLEET_MANAGER', 'ROLE_SUPER_MANAGER')")
    @Operation(summary = "Get event details")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getEventById(id)));
    }

    @PostMapping("/sync/offline-data")
    @PreAuthorize("hasRole('ROLE_DRIVER')")
    @Operation(summary = "Triggered only when the mobile app reconnects after a network drop")
    public ResponseEntity<ApiResponse<Void>> syncOfflineData(
            @Valid @RequestBody OfflineSyncRequest request) {
        eventService.syncOfflineData(request);
        return ResponseEntity.accepted()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Offline sync data accepted for processing")
                        .build());
    }
}
