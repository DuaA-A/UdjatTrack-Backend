package com.udjattrack.controller;

import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.TelemetryRecordResponse;
import com.udjattrack.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TelemetryWebSocketController {

    private final TelemetryService telemetryService;
    @MessageMapping("/telemetry.ingest.{tripId}")
    public void ingestTelemetryViaWebSocket(@DestinationVariable UUID tripId,
                                            @Payload TelemetryRequest request, 
                                            SimpMessageHeaderAccessor headerAccessor, 
                                            Principal principal) {
        log.debug("Received telemetry via WebSocket for trip {}: {}", tripId, request.location());
        
        try {
            TelemetryRecordResponse response = telemetryService.ingestTelemetry(tripId, request);
            log.debug("Successfully processed WebSocket telemetry: {}", response.getId());
        } catch (Exception e) {
            log.error("Failed to ingest telemetry via WebSocket: {}", e.getMessage(), e);
        }
    }
}
