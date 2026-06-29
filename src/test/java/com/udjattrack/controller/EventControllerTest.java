package com.udjattrack.controller;

import com.udjattrack.dto.request.EventRequest;
import com.udjattrack.dto.request.OfflineSyncRequest;
import com.udjattrack.dto.response.EventResponse;
import com.udjattrack.security.JwtUtil;
import com.udjattrack.security.UserDetailsServiceImpl;
import com.udjattrack.service.EventService;
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

@WebMvcTest(value = EventController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private EventService eventService;

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
    void reportEvent_shouldReturnCreated() throws Exception {
        UUID tripId = UUID.randomUUID();
        EventResponse response = EventResponse.builder()
                .eventId(UUID.randomUUID())
                .eventType("CRASH")
                .build();
        Mockito.when(eventService.reportEvent(eq(tripId), any())).thenReturn(response);

        EventRequest request = new EventRequest(java.time.OffsetDateTime.now(), "CRASH", null);

        mockMvc.perform(post("/trips/" + tripId + "/events")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventType").value("CRASH"));
    }

    @Test
    void syncOfflineData_shouldReturn202() throws Exception {
        OfflineSyncRequest request = new OfflineSyncRequest(UUID.randomUUID(), List.of(), List.of());

        mockMvc.perform(post("/sync/offline-data")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isAccepted());

        Mockito.verify(eventService).syncOfflineData(any(OfflineSyncRequest.class));
    }
}
