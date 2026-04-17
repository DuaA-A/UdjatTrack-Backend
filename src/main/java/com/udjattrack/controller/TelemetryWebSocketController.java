package com.udjattrack.controller;

import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.response.TelemetryRecordResponse;
import com.udjattrack.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TelemetryWebSocketController {

    private final TelemetryService telemetryService;

    /**
     * WebSocket endpoint for high-frequency telemetry ingestion from IoT devices or mobile apps.
     * The mobile app connects via WebSocket and sends messages to "/app/telemetry.ingest".
     */
    @MessageMapping("/telemetry.ingest")
    public void ingestTelemetryViaWebSocket(@Payload TelemetryRequest request, 
                                            SimpMessageHeaderAccessor headerAccessor, 
                                            Principal principal) {
        log.debug("Received telemetry via WebSocket for trip {}: {}", request.tripId(), request.location());
        
        try {
            // Process the telemetry (this will save to TSDB and push update to fleet manager dashboard)
            TelemetryRecordResponse response = telemetryService.ingestTelemetry(request);
            log.debug("Successfully processed WebSocket telemetry: {}", response.getId());
        } catch (Exception e) {
            log.error("Failed to ingest telemetry via WebSocket: {}", e.getMessage(), e);
            // Optionally, handle error sending back to a user-specific error queue if needed
        }
    }
}
