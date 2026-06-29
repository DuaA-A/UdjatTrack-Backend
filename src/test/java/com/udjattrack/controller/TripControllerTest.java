package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateTripRequest;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.entity.enums.TripStatus;
import com.udjattrack.security.JwtUtil;
import com.udjattrack.security.UserDetailsServiceImpl;
import com.udjattrack.service.EmergencyService;
import com.udjattrack.service.EventService;
import com.udjattrack.service.TripService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
// Removed reference to JpaAuditingAutoConfiguration (not present on classpath)
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import com.udjattrack.config.SecurityConfig;
import com.udjattrack.security.JwtAuthFilter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TripController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@Import(SecurityConfig.class)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private TripService tripService;

    @SuppressWarnings("unused")
    @MockBean
    private EmergencyService emergencyService;

    @SuppressWarnings("unused")
    @MockBean
    private EventService eventService;

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
    void createTripAsFleetManager_shouldReturnCreated() throws Exception {
        UUID managerId = UUID.randomUUID();
        TripResponse response = TripResponse.builder()
                .status(TripStatus.PLANNED)
                .build();

        Mockito.when(tripService.createTrip(any(CreateTripRequest.class))).thenReturn(response);

        CreateTripRequest request = new CreateTripRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Cairo",
                "Alexandria",
                "Route 1",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(5)
        );

        mockMvc.perform(post("/trips")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(managerId, "manager@example.com", "ROLE_FLEET_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PLANNED"));
    }

    @Test
    void startTripAsDriver_shouldReturnOk() throws Exception {
        UUID tripId = UUID.randomUUID();
        TripResponse response = TripResponse.builder()
                .status(TripStatus.ONGOING)
                .build();
        Mockito.when(tripService.startTrip(eq(tripId))).thenReturn(response);

        mockMvc.perform(post("/trips/" + tripId + "/start")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ONGOING"));
    }

    @Test
    void getTripProgress_whenNotFound_shouldReturnNotFound() throws Exception {
        UUID tripId = UUID.randomUUID();
        Mockito.when(tripService.trackTripProgress(eq(tripId)))
                .thenThrow(new com.udjattrack.exception.ResourceNotFoundException("Trip", "id", tripId.toString()));

        mockMvc.perform(get("/trips/" + tripId + "/progress")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listTripsAsDriver_withoutDriverId_shouldUseCurrentUserId() throws Exception {
        UUID driverId = UUID.randomUUID();
        TripResponse response = TripResponse.builder()
                .tripId(UUID.randomUUID())
                .build();
        Mockito.when(tripService.getTripsByDriver(eq(driverId), org.mockito.ArgumentMatchers.nullable(String.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/trips")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(driverId, "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void createTripDriverAlreadyOnTrip_shouldReturn422() throws Exception {
        Mockito.when(tripService.createTrip(any(CreateTripRequest.class)))
                .thenThrow(new com.udjattrack.exception.BusinessException("DRIVER_ALREADY_ON_TRIP"));

        CreateTripRequest request = new CreateTripRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Cairo",
                "Alexandria",
                "Route 1",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(5)
        );

        mockMvc.perform(post("/trips")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createTripAsDriver_shouldReturn403() throws Exception {
        CreateTripRequest request = new CreateTripRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Cairo",
                "Alexandria",
                "Route 1",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(5)
        );

        mockMvc.perform(post("/trips")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void startTripAlreadyOngoing_shouldReturn422() throws Exception {
        UUID tripId = UUID.randomUUID();
        Mockito.when(tripService.startTrip(eq(tripId)))
                .thenThrow(new com.udjattrack.exception.BusinessException("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/trips/" + tripId + "/start")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void startTripAsFleetManager_shouldReturn403() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(post("/trips/" + tripId + "/start")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void pauseTripSuccess_shouldReturnOk() throws Exception {
        UUID tripId = UUID.randomUUID();
        TripResponse response = TripResponse.builder().status(TripStatus.ON_BREAK).build();
        Mockito.when(tripService.stopTrip(eq(tripId), any())).thenReturn(response);

        mockMvc.perform(post("/trips/" + tripId + "/Paused")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ON_BREAK"));
    }

    @Test
    void pauseTripInvalidState_shouldReturn422() throws Exception {
        UUID tripId = UUID.randomUUID();
        Mockito.when(tripService.stopTrip(eq(tripId), any()))
                .thenThrow(new com.udjattrack.exception.BusinessException("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/trips/" + tripId + "/Paused")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void resumeTripSuccess_shouldReturnOk() throws Exception {
        UUID tripId = UUID.randomUUID();
        TripResponse response = TripResponse.builder().status(TripStatus.ONGOING).build();
        Mockito.when(tripService.resumeTrip(eq(tripId), any())).thenReturn(response);

        mockMvc.perform(post("/trips/" + tripId + "/Resumed")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ONGOING"));
    }

    @Test
    void resumeTripInvalidState_shouldReturn422() throws Exception {
        UUID tripId = UUID.randomUUID();
        Mockito.when(tripService.resumeTrip(eq(tripId), any()))
                .thenThrow(new com.udjattrack.exception.BusinessException("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/trips/" + tripId + "/Resumed")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void endTripSuccess_shouldReturnOk() throws Exception {
        UUID tripId = UUID.randomUUID();
        TripResponse response = TripResponse.builder().status(TripStatus.FINISHED).build();
        Mockito.when(tripService.completeTrip(eq(tripId))).thenReturn(response);

        mockMvc.perform(post("/trips/" + tripId + "/end")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FINISHED"));
    }

    @Test
    void endTripPlanned_shouldReturn422() throws Exception {
        UUID tripId = UUID.randomUUID();
        Mockito.when(tripService.completeTrip(eq(tripId)))
                .thenThrow(new com.udjattrack.exception.BusinessException("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/trips/" + tripId + "/end")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelTripSuccess_shouldReturnOk() throws Exception {
        UUID tripId = UUID.randomUUID();
        TripResponse response = TripResponse.builder().status(TripStatus.CANCELLED).build();
        Mockito.when(tripService.cancelTrip(eq(tripId))).thenReturn(response);

        mockMvc.perform(post("/trips/" + tripId + "/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void cancelTripFinished_shouldReturn422() throws Exception {
        UUID tripId = UUID.randomUUID();
        Mockito.when(tripService.cancelTrip(eq(tripId)))
                .thenThrow(new com.udjattrack.exception.BusinessException("INVALID_STATE_TRANSITION"));

        mockMvc.perform(post("/trips/" + tripId + "/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelTripAsDriver_shouldReturn403() throws Exception {
        UUID tripId = UUID.randomUUID();

        mockMvc.perform(post("/trips/" + tripId + "/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTripByIdNotFound_shouldReturn404() throws Exception {
        UUID tripId = UUID.randomUUID();
        Mockito.when(tripService.getTripById(eq(tripId)))
                .thenThrow(new com.udjattrack.exception.ResourceNotFoundException("Trip", "id", tripId.toString()));

        mockMvc.perform(get("/trips/" + tripId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTripTimeline_shouldReturnTimeline() throws Exception {
        UUID tripId = UUID.randomUUID();
        TripTimelineResponse timeline = TripTimelineResponse.builder()
                .timeline(List.of())
                .build();
        Mockito.when(tripService.getTripTimeline(eq(tripId), any(), any())).thenReturn(timeline);

        mockMvc.perform(get("/trips/" + tripId + "/timeline")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
