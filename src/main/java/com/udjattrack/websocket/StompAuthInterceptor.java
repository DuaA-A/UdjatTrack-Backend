package com.udjattrack.websocket;

import com.udjattrack.security.JwtUtil;
import com.udjattrack.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * STOMP channel interceptor for JWT authentication.
 * Extracts JWT from the "Authorization" header in STOMP CONNECT frames
 * and sets the Spring Security context so that subscriptions to
 * authenticated topics are permitted.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null) {
                authHeader = accessor.getFirstNativeHeader("authorization");
            }
            
            log.debug("WebSocket CONNECT attempt. Header present: {}", authHeader != null);

            if (authHeader != null) {
                String jwt = authHeader.startsWith(BEARER_PREFIX) ? 
                             authHeader.substring(BEARER_PREFIX.length()) : authHeader;
                try {
                    String email = jwtUtil.extractEmail(jwt);
                    log.debug("Extracted email from JWT: {}", email);
                    if (email != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                        if (jwtUtil.isTokenValid(jwt, userDetails.getUsername())) {
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                            accessor.setUser(authToken);
                            log.info("WebSocket STOMP CONNECT authenticated for user: {}", email);
                        } else {
                            log.warn("Invalid JWT token for user: {}", email);
                        }
                    }
                } catch (Exception e) {
                    log.error("WebSocket STOMP authentication error: {}", e.getMessage(), e);
                }
            } else {
                log.warn("No Authorization header found in STOMP CONNECT frame");
            }
        }

        return message;
    }
}
