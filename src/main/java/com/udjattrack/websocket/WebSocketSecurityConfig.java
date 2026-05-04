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
    public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages
                .nullDestMatcher().authenticated()
                .simpSubscribeDestMatchers("/topic/**").authenticated()
                .simpDestMatchers("/app/**").authenticated()
                .simpTypeMatchers(org.springframework.messaging.simp.SimpMessageType.DISCONNECT, 
                                 org.springframework.messaging.simp.SimpMessageType.UNSUBSCRIBE).permitAll()
                .anyMessage().denyAll();
        return messages.build();
    }

    // Disable CSRF for simpler WebSocket testing/development
    @Bean("csrfChannelInterceptor")
    public Object csrfChannelInterceptor() {
        return null;
    }
}
