package com.udjattrack.websocket;

import com.udjattrack.dto.websocket.TripStateUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.udjattrack.dto.websocket.AlertEventMessage;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    public void publishLiveTracking(UUID fleetId, TripStateUpdateMessage message) {
        String destination = "/topic/fleet/" + fleetId + "/telemetry";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Published live-tracking update to {}", destination);
    }

    public void publishAlert(UUID fleetId, AlertEventMessage message) {
        String destination = "/topic/fleet/" + fleetId + "/alerts";
        messagingTemplate.convertAndSend(destination, message);
        log.info("Published alert event to {}", destination);
    }

    public void publishDriverStatus(UUID driverId, Object statusUpdate) {
        String destination = "/topic/driver/" + driverId + "/status";
        messagingTemplate.convertAndSend(destination, statusUpdate);
        log.debug("Published status update to driver {}", driverId);
    }
    public void publishTripMonitoring(UUID fleetId, com.udjattrack.dto.websocket.TripMonitoringMessage message) {
        String destination = "/topic/fleet/" + fleetId + "/trip-monitoring";
        messagingTemplate.convertAndSend(destination, message);
        log.info("Published trip-monitoring update to {}", destination);
    }
}
