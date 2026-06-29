package com.udjattrack.controller;

import com.udjattrack.dto.request.CreateDriverRequest;
import com.udjattrack.dto.response.DriverResponse;
import com.udjattrack.security.JwtUtil;
import com.udjattrack.security.UserDetailsServiceImpl;
import com.udjattrack.service.FleetManagementService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import com.udjattrack.config.SecurityConfig;
import com.udjattrack.security.JwtAuthFilter;

import java.util.UUID;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = DriverController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@Import(SecurityConfig.class)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private FleetManagementService fleetManagementService;

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
    void createDriverAsFleetManager_shouldReturnCreated() throws Exception {
        UUID managerId = UUID.randomUUID();
        DriverResponse response = DriverResponse.builder()
                .email("driver@example.com")
                .userId(UUID.randomUUID())
                .build();
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
        DriverResponse response = DriverResponse.builder()
                .userId(driverId)
                .email("driver@example.com")
                .build();
        Mockito.when(fleetManagementService.getDriverById(eq(driverId))).thenReturn(response);

        mockMvc.perform(get("/drivers/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(driverId, "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("driver@example.com"));
    }

    @Test
    void uploadDriverPhoto_shouldReturnUpdatedDriver() throws Exception {
        UUID driverId = UUID.randomUUID();
        DriverResponse response = DriverResponse.builder()
                .userId(driverId)
                .email("driver@example.com")
                .build();
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

    @Test
    void createDriverDuplicateEmail_shouldReturn409() throws Exception {
        UUID managerId = UUID.randomUUID();
        Mockito.when(fleetManagementService.createDriver(eq(managerId), any(CreateDriverRequest.class)))
                .thenThrow(new com.udjattrack.exception.DuplicateResourceException("Driver", "email", "driver@example.com"));

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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createDriverAsDriver_shouldReturn403() throws Exception {
        CreateDriverRequest request = new CreateDriverRequest(
                "Driver One",
                "driver@example.com",
                "Password123",
                "LN-12345",
                "01000000000"
        );

        mockMvc.perform(post("/drivers")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDriversAsFleetManager_shouldReturnList() throws Exception {
        UUID managerId = UUID.randomUUID();
        DriverResponse response = DriverResponse.builder().email("driver@example.com").build();
        Mockito.when(fleetManagementService.getDriversByManager(eq(managerId))).thenReturn(List.of(response));

        mockMvc.perform(get("/drivers")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(managerId, "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void getDriversAsDriver_shouldReturn403() throws Exception {
        mockMvc.perform(get("/drivers")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDriverByIdNotFound_shouldReturn404() throws Exception {
        UUID driverId = UUID.randomUUID();
        Mockito.when(fleetManagementService.getDriverById(eq(driverId)))
                .thenThrow(new com.udjattrack.exception.ResourceNotFoundException("Driver", "id", driverId.toString()));

        mockMvc.perform(get("/drivers/" + driverId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteDriverWithOngoingTrip_shouldReturn422() throws Exception {
        UUID driverId = UUID.randomUUID();
        Mockito.doThrow(new com.udjattrack.exception.BusinessException("Driver is currently on an ongoing trip"))
                .when(fleetManagementService).deleteDriver(eq(driverId));

        mockMvc.perform(delete("/drivers/" + driverId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteDriverAsDriver_shouldReturn403() throws Exception {
        UUID driverId = UUID.randomUUID();
        mockMvc.perform(delete("/drivers/" + driverId)
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isForbidden());
    }
}
