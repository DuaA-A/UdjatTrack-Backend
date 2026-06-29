package com.udjattrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udjattrack.dto.request.LoginRequest;
import com.udjattrack.dto.request.ForgotPasswordRequest;
import com.udjattrack.dto.request.CreateSuperManagerRequest;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.AuthResponse;
import com.udjattrack.dto.response.SuperManagerSignupResponse;
import com.udjattrack.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void loginSuccess_shouldReturnAuthResponse() throws Exception {
        AuthResponse authResponse = Mockito.mock(AuthResponse.class);
        Mockito.when(authResponse.getAccessToken()).thenReturn("access-token");
        Mockito.when(authResponse.getRefreshToken()).thenReturn("refresh-token");
        Mockito.when(authResponse.getTokenType()).thenReturn("Bearer");
        Mockito.when(authResponse.getExpiresIn()).thenReturn(900L);
        Mockito.when(authResponse.getUserId()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(authResponse.getEmail()).thenReturn("user@example.com");
        Mockito.when(authResponse.getName()).thenReturn("Test User");

        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new LoginRequest("user@example.com", "password123", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void loginInvalidEmail_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new LoginRequest("not-an-email", "pass", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.email").value("Must be a valid email address"));
    }

    @Test
    void forgotPassword_shouldReturnSuccessForRegisteredEmail() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new ForgotPasswordRequest("user@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("If this email is registered, a reset code has been sent."));

        Mockito.verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    void signupSuperManager_shouldReturnCreated() throws Exception {
        SuperManagerSignupResponse response = Mockito.mock(SuperManagerSignupResponse.class);
        Mockito.when(response.getEmail()).thenReturn("super@example.com");
        Mockito.when(response.getStatus()).thenReturn("ACTIVE");

        Mockito.when(authService.signupSuperManager(any(CreateSuperManagerRequest.class))).thenReturn(response);

        CreateSuperManagerRequest request = new CreateSuperManagerRequest(
                "Super User",
                "super@example.com",
                "Password123!"
        );

        mockMvc.perform(post("/auth/signup/super-manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("super@example.com"));
    }
}
