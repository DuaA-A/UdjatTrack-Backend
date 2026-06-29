package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateTripRequest;
import com.udjattrack.dto.response.TripResponse;
import com.udjattrack.service.EmergencyService;
import com.udjattrack.service.EventService;
import com.udjattrack.service.TripService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @SuppressWarnings("unused")
    @MockBean
    private EmergencyService emergencyService;

    @SuppressWarnings("unused")
    @MockBean
    private EventService eventService;

    @Test
    void createTripAsFleetManager_shouldReturnCreated() throws Exception {
        UUID managerId = UUID.randomUUID();
        TripResponse response = Mockito.mock(TripResponse.class);
        Mockito.when(response.getStatus()).thenReturn(com.udjattrack.entity.enums.TripStatus.PLANNED);

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
        TripResponse response = new TripResponse() {
            public com.udjattrack.entity.enums.TripStatus getStatus() { return com.udjattrack.entity.enums.TripStatus.ONGOING; }
        };
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
        TripResponse response = new TripResponse() {
            public UUID getTripId() { return UUID.randomUUID(); }
        };
        Mockito.when(tripService.getTripsByDriver(eq(driverId), any(String.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/trips")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(driverId, "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
