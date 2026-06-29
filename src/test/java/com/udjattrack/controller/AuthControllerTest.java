package com.udjattrack.controller;

import com.udjattrack.dto.request.LoginRequest;
import com.udjattrack.dto.request.ForgotPasswordRequest;
import com.udjattrack.dto.request.CreateSuperManagerRequest;
import com.udjattrack.dto.response.AuthResponse;
import com.udjattrack.dto.response.SuperManagerSignupResponse;
import com.udjattrack.security.JwtUtil;
import com.udjattrack.security.UserDetailsServiceImpl;
import com.udjattrack.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
// Removed reference to JpaAuditingAutoConfiguration (not present on classpath)
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @SuppressWarnings("unused")
    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @SuppressWarnings("unused")
    @MockBean
    private JwtUtil jwtUtil;

    @SuppressWarnings("unused")
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loginSuccess_shouldReturnAuthResponse() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .userId(UUID.randomUUID().toString())
                .email("user@example.com")
                .name("Test User")
                .build();

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
        SuperManagerSignupResponse response = SuperManagerSignupResponse.builder()
                .email("super@example.com")
                .status("ACTIVE")
                .build();

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
