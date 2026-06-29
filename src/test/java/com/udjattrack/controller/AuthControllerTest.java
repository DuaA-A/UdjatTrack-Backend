package com.udjattrack.controller;

import com.udjattrack.dto.request.LoginRequest;
import com.udjattrack.dto.request.ForgotPasswordRequest;
import com.udjattrack.dto.request.CreateSuperManagerRequest;
import com.udjattrack.dto.request.CreateFleetManagerRequest;
import com.udjattrack.dto.request.RefreshTokenRequest;
import com.udjattrack.dto.request.ResetPasswordRequest;
import com.udjattrack.dto.request.VerifyOtpRequest;
import com.udjattrack.dto.response.AuthResponse;
import com.udjattrack.dto.response.SuperManagerSignupResponse;
import com.udjattrack.dto.response.FleetManagerSignupResponse;
import com.udjattrack.security.JwtUtil;
import com.udjattrack.security.UserDetailsServiceImpl;
import com.udjattrack.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import com.udjattrack.config.SecurityConfig;
import com.udjattrack.security.JwtAuthFilter;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

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

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        Mockito.doAnswer(invocation -> {
            ((jakarta.servlet.FilterChain) invocation.getArgument(2))
                    .doFilter(
                            (jakarta.servlet.ServletRequest) invocation.getArgument(0),
                            (jakarta.servlet.ServletResponse) invocation.getArgument(1)
                    );
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

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
    void loginWrongPassword_shouldReturn401() throws Exception {
        Mockito.when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Wrong password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new LoginRequest("user@example.com", "wrongpass", null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginPendingApproval_shouldReturn401() throws Exception {
        Mockito.when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.security.authentication.DisabledException("FLEET_MANAGER with ACCOUNT_PENDING_APPROVAL status is rejected"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new LoginRequest("pending@example.com", "password123", null))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void refreshTokenSuccess_shouldReturnNewAccessToken() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .build();
        Mockito.when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new RefreshTokenRequest("valid-refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    void refreshTokenExpired_shouldReturn401() throws Exception {
        Mockito.when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new org.springframework.security.authentication.CredentialsExpiredException("Refresh token has expired"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new RefreshTokenRequest("expired-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void logoutSuccess_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new RefreshTokenRequest("token-to-revoke"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        Mockito.verify(authService).logout(eq("token-to-revoke"));
    }

    @Test
    void resetPasswordSuccess_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new ResetPasswordRequest("user@example.com", "123456", "NewPassword123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Mockito.verify(authService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPasswordWrongOtp_shouldReturn400() throws Exception {
        Mockito.doThrow(new com.udjattrack.exception.InvalidOtpException("Invalid OTP code"))
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(new ResetPasswordRequest("user@example.com", "111111", "NewPassword123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void signupFleetManagerSuccess_shouldReturnCreated() throws Exception {
        FleetManagerSignupResponse response = FleetManagerSignupResponse.builder()
                .fleetManagerId(UUID.randomUUID())
                .status("PENDING_APPROVAL")
                .build();
        Mockito.when(authService.signupFleetManager(any(CreateFleetManagerRequest.class))).thenReturn(response);

        CreateFleetManagerRequest request = new CreateFleetManagerRequest(
                "Fleet Manager",
                "manager@example.com",
                "Password123!",
                "Company A"
        );

        mockMvc.perform(post("/auth/signup/fleet-manager")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "super@example.com", "ROLE_SUPER_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));
    }

    @Test
    void signupFleetManagerUnauthorizedRole_shouldReturn403() throws Exception {
        CreateFleetManagerRequest request = new CreateFleetManagerRequest(
                "Fleet Manager",
                "manager@example.com",
                "Password123!",
                "Company A"
        );

        mockMvc.perform(post("/auth/signup/fleet-manager")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isForbidden());
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
