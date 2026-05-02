package com.udjattrack.websocket;

import com.udjattrack.dto.websocket.AlertEventMessage;
import com.udjattrack.dto.websocket.TripStateUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Section 13.1: Live Map Tracking
     * Pushes to fleet managers for real-time dashboard updates.
     */
    public void publishLiveTracking(UUID managerId, TripStateUpdateMessage message) {
        String destination = "/topic/fleet/" + managerId + "/telemetry";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Published live-tracking update to {}", destination);
    }

    /**
     * Section 13.2: Alert Notifications
     * Pushes to fleet managers for critical alert popups (Accidents, SOS, etc.)
     */
    public void publishAlert(UUID managerId, AlertEventMessage message) {
        String destination = "/topic/fleet/" + managerId + "/alerts";
        messagingTemplate.convertAndSend(destination, message);
        log.info("Published alert event to {}", destination);
    }

    /**
     * Section 13.3: Driver Personal Channel
     * Pushed to the mobile app for status confirmations and private messages.
     */
    public void publishDriverStatus(UUID driverId, Object statusUpdate) {
        String destination = "/topic/driver/" + driverId + "/status";
        messagingTemplate.convertAndSend(destination, statusUpdate);
        log.debug("Published status update to driver {}", driverId);
    }
}
