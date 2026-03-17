package com.udjattrack.dto.response;

import com.udjattrack.entity.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

/**
 * Auth response returned after successful login or token refresh.
 */
@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;        // seconds
    private String userId;
    private String email;
    private String name;
    private UserRole role;
}
