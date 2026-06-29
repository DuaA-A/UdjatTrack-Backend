package com.udjattrack.controller;

import com.udjattrack.dto.request.AddVehicleRequest;
import com.udjattrack.dto.response.VehicleResponse;
import com.udjattrack.dto.response.VehicleWithDriverResponse;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import com.udjattrack.config.SecurityConfig;
import com.udjattrack.security.JwtAuthFilter;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = VehicleController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class
        })
@Import(SecurityConfig.class)
class VehicleControllerTest {

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
    void addVehicleAsFleetManager_shouldReturnCreated() throws Exception {
        UUID managerId = UUID.randomUUID();
        VehicleResponse response = VehicleResponse.builder()
                .plateNumber("ABC123")
                .build();
        Mockito.when(fleetManagementService.addVehicle(eq(managerId), any(AddVehicleRequest.class))).thenReturn(response);

        AddVehicleRequest request = new AddVehicleRequest(
                "ABC123",
                "Model X",
                2024
        );

        mockMvc.perform(post("/vehicles")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(managerId, "manager@example.com", "ROLE_FLEET_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestUtils.toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.plateNumber").value("ABC123"));
    }

    @Test
    void getVehicleWithDriver_shouldReturnVehicleWithDriver() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        com.udjattrack.dto.response.VehicleResponse vehicleResponse = com.udjattrack.dto.response.VehicleResponse.builder()
                .vehicleId(vehicleId)
                .build();
        VehicleWithDriverResponse response = VehicleWithDriverResponse.builder()
                .vehicle(vehicleResponse)
                .build();
        Mockito.when(fleetManagementService.getVehicleWithDriver(eq(vehicleId))).thenReturn(response);

        mockMvc.perform(get("/vehicles/" + vehicleId + "/with-driver")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "manager@example.com", "ROLE_FLEET_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vehicle.vehicleId").value(vehicleId.toString()));
    }

    @Test
    void getMaintenanceLogs_shouldReturnMockLogs() throws Exception {
        UUID vehicleId = UUID.randomUUID();

        mockMvc.perform(get("/vehicles/" + vehicleId + "/maintenance-logs")
                        .with(SecurityMockMvcRequestPostProcessors.user(ControllerTestUtils.securityUser(UUID.randomUUID(), "driver@example.com", "ROLE_DRIVER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }
}
