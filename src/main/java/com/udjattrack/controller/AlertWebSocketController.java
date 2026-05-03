package com.udjattrack.controller;

import com.udjattrack.dto.response.AlertResponse;
import com.udjattrack.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AlertWebSocketController {

    private final AlertService alertService;

    /**
     * Section 13.2: Live Alerts
     * When a fleet manager subscribes to "/topic/fleet/{fleetId}/alerts",
     * this mapping can optionally provide the initial state (unacknowledged alerts).
     * Note: Standard STOMP brokers handle the subscription; this is for the initial push.
     */
    @SubscribeMapping("/fleet.{fleetId}.alerts")
    public List<AlertResponse> subscribeToAlerts(@DestinationVariable UUID fleetId) {
        log.info("Fleet Manager {} subscribed to real-time alerts", fleetId);
        // Returning the list here sends it ONLY to the subscriber who just joined.
        return alertService.getUnacknowledgedAlerts(fleetId);
    }
}
