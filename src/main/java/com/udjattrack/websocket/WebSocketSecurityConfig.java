package com.udjattrack.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages
                .simpTypeMatchers(org.springframework.messaging.simp.SimpMessageType.CONNECT, 
                                 org.springframework.messaging.simp.SimpMessageType.HEARTBEAT,
                                 org.springframework.messaging.simp.SimpMessageType.DISCONNECT).permitAll()
                .anyMessage().permitAll();
        return messages.build();
    }

    @Bean("csrfChannelInterceptor")
    public org.springframework.messaging.support.ChannelInterceptor csrfChannelInterceptor() {
        return new org.springframework.messaging.support.ChannelInterceptor() {};
    }
}