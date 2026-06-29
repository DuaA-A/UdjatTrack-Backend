package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateAlertRequest;
import com.udjattrack.dto.response.AlertResponse;
import com.udjattrack.dto.response.AlertSummaryResponse;
import com.udjattrack.security.JwtUtil;
import com.udjattrack.security.UserDetailsServiceImpl;
import com.udjattrack.service.AlertService;
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

@WebMvcTest(value = AlertController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@Import(SecurityConfig.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private AlertService alertService;

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
    void getAlerts_shouldReturnAlertList() throws Exception {
        UUID managerId = UUID.randomUUID();
        AlertResponse alert = AlertResponse.builder()
                .alertId(UUID.randomUUID())
                .severity("CRITICAL")
                .build();
        Mockito.when(alertService.getAllAlertsByFleetManager(eq(managerId))).thenReturn(List.of(alert));

        mockMvc.perform(get("/alerts")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(managerId, "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void getAlertsUnacknowledged_shouldReturnUnacknowledgedAlerts() throws Exception {
        UUID managerId = UUID.randomUUID();
        AlertResponse alert = AlertResponse.builder()
                .alertId(UUID.randomUUID())
                .status("UNACKNOWLEDGED")
                .build();
        Mockito.when(alertService.getUnacknowledgedAlerts(eq(managerId))).thenReturn(List.of(alert));

        mockMvc.perform(get("/alerts?status=UNACKNOWLEDGED")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(managerId, "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("UNACKNOWLEDGED"));
    }

    @Test
    void acknowledgeAlert_shouldReturnUpdatedAlert() throws Exception {
        UUID alertId = UUID.randomUUID();
        AlertResponse alert = AlertResponse.builder()
                .alertId(alertId)
                .status("ACKNOWLEDGED")
                .build();
        Mockito.when(alertService.acknowledgeAlert(eq(alertId))).thenReturn(alert);

        mockMvc.perform(post("/alerts/" + alertId + "/ack")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));
    }

    @Test
    void getAlertById_shouldReturnAlertDetails() throws Exception {
        UUID alertId = UUID.randomUUID();
        AlertResponse alert = AlertResponse.builder()
                .alertId(alertId)
                .build();
        Mockito.when(alertService.getAlertById(eq(alertId))).thenReturn(alert);

        mockMvc.perform(get("/alerts/" + alertId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
