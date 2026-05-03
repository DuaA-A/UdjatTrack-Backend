package com.udjattrack.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${application.websocket.endpoint:/ws}")
    private String endpoint;

    @Value("${application.websocket.app-prefix:/app}")
    private String appPrefix;

    @Value("${application.websocket.topic-prefix:/topic}")
    private String topicPrefix;

    @Value("${application.websocket.allowed-origins:*}")
    private String allowedOrigins;

    private final StompAuthInterceptor stompAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic for broadcasts (server -> client)
        config.enableSimpleBroker(topicPrefix);
        // /app for incoming messages (client -> server)
        config.setApplicationDestinationPrefixes(appPrefix);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registration of the endpoint /api/v1/ws (prefixed by context-path)
        registry.addEndpoint(endpoint)
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();

        registry.addEndpoint(endpoint)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register the JWT authentication interceptor for STOMP connections
        registration.interceptors(stompAuthInterceptor);
    }
}
