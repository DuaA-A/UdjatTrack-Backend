package com.udjattrack.controller;

import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.NotificationResponse;
import com.udjattrack.security.JwtUtil;
import com.udjattrack.security.UserDetailsServiceImpl;
import com.udjattrack.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = NotificationController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private NotificationService notificationService;

    @SuppressWarnings("unused")
    @MockBean
    private JwtUtil jwtUtil;

    @SuppressWarnings("unused")
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
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
    void getMyNotifications_shouldReturnList() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationResponse response = NotificationResponse.builder()
                .id(UUID.randomUUID())
                .title("TRIP_ASSIGNED")
                .isRead(false)
                .build();
        Mockito.when(notificationService.getUserNotifications(eq(userId))).thenReturn(List.of(response));

        mockMvc.perform(get("/notifications")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(userId, "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void markAsRead_shouldReturnSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        mockMvc.perform(patch("/notifications/" + notificationId + "/read")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(userId, "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Mockito.verify(notificationService).markAsRead(eq(notificationId), eq(userId));
    }
}
