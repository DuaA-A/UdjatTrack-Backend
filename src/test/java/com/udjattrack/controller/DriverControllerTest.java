package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateDriverRequest;
import com.udjattrack.dto.request.CreateDependentRequest;
import com.udjattrack.dto.response.DriverResponse;
import com.udjattrack.dto.response.DependentResponse;
import com.udjattrack.service.FleetManagementService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverController.class)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FleetManagementService fleetManagementService;

    @Test
    void createDriverAsFleetManager_shouldReturnCreated() throws Exception {
        UUID managerId = UUID.randomUUID();
        DriverResponse response = Mockito.mock(DriverResponse.class);
        Mockito.when(response.getEmail()).thenReturn("driver@example.com");
        Mockito.when(response.getUserId()).thenReturn(UUID.randomUUID());
        Mockito.when(fleetManagementService.createDriver(eq(managerId), any(CreateDriverRequest.class))).thenReturn(response);

        CreateDriverRequest request = new CreateDriverRequest(
                "Driver One",
                "driver@example.com",
                "Password123",
                "LN-12345",
                "01000000000"
        );

        mockMvc.perform(post("/drivers")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(managerId, "manager@example.com", "ROLE_FLEET_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("driver@example.com"));
    }

    @Test
    void getMeAsDriver_shouldReturnProfile() throws Exception {
        UUID driverId = UUID.randomUUID();
        DriverResponse response = Mockito.mock(DriverResponse.class);
        Mockito.when(response.getUserId()).thenReturn(driverId);
        Mockito.when(response.getEmail()).thenReturn("driver@example.com");
        Mockito.when(fleetManagementService.getDriverById(eq(driverId))).thenReturn(response);

        mockMvc.perform(get("/drivers/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(driverId, "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("driver@example.com"));
    }

    @Test
    void uploadDriverPhoto_shouldReturnUpdatedDriver() throws Exception {
        UUID driverId = UUID.randomUUID();
        DriverResponse response = Mockito.mock(DriverResponse.class);
        Mockito.when(response.getUserId()).thenReturn(driverId);
        Mockito.when(response.getEmail()).thenReturn("driver@example.com");
        Mockito.when(fleetManagementService.uploadDriverPhoto(eq(driverId), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/drivers/" + driverId + "/photo")
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(driverId, "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(driverId.toString()));
    }

    @Test
    void deleteDriverAsFleetManager_shouldReturnOk() throws Exception {
        UUID driverId = UUID.randomUUID();

        mockMvc.perform(delete("/drivers/" + driverId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Mockito.verify(fleetManagementService).deleteDriver(eq(driverId));
    }
}
