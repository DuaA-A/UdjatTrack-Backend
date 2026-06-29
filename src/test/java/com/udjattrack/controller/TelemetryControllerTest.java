package com.udjattrack.controller;

import com.udjattrack.dto.request.TelemetryRequest;
import com.udjattrack.dto.request.TelemetryBatchRequest;
import com.udjattrack.dto.response.TelemetryRecordResponse;
import com.udjattrack.security.JwtUtil;
import com.udjattrack.security.UserDetailsServiceImpl;
import com.udjattrack.service.TelemetryService;
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

@WebMvcTest(value = TelemetryController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@Import(SecurityConfig.class)
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private TelemetryService telemetryService;

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
    void ingestTelemetry_shouldReturn202() throws Exception {
        UUID tripId = UUID.randomUUID();
        TelemetryRequest request = new TelemetryRequest(
                java.time.OffsetDateTime.now(),
                new com.udjattrack.dto.request.LocationDTO(12.34, 56.78),
                60.5,
                com.udjattrack.entity.enums.DriverState.NORMAL,
                null
        );

        mockMvc.perform(post("/trips/" + tripId + "/telemetry")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isAccepted());

        Mockito.verify(telemetryService).ingestTelemetry(eq(tripId), any(TelemetryRequest.class));
    }

    @Test
    void getTelemetryByTrip_shouldReturnHistory() throws Exception {
        UUID tripId = UUID.randomUUID();
        Mockito.when(telemetryService.getTelemetryByTrip(eq(tripId))).thenReturn(List.of());

        mockMvc.perform(get("/trips/" + tripId + "/telemetry")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
