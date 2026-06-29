package com.udjattrack.controller;

import com.udjattrack.security.SecurityUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

public final class ControllerTestUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ControllerTestUtils() {
    }

    public static SecurityUser securityUser(UUID userId, String username, String role) {
        return new SecurityUser(
                username,
                "password",
                List.of(new SimpleGrantedAuthority(role)),
                userId
        );
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }
}
