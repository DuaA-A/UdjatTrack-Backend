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

    public void publishTelemetry(UUID managerId, TripStateUpdateMessage message) {
        String destination = "/topic/fleet/" + managerId + "/telemetry";
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Published telemetry update to {}", destination);
    }

    public void publishAlert(UUID managerId, AlertEventMessage message) {
        String destination = "/topic/fleet/" + managerId + "/alerts";
        messagingTemplate.convertAndSend(destination, message);
        log.info("Published alert event to {}", destination);
    }

    public void publishSos(UUID managerId, AlertEventMessage message) {
        String destination = "/topic/fleet/" + managerId + "/sos";
        messagingTemplate.convertAndSend(destination, message);
        log.warn("Published SOS event to {}", destination);
    }
}
