package com.udjattrack.service;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.*;

import java.util.List;
import java.util.UUID;

/**
 * Fleet management service — CRUD for fleet managers, drivers, and vehicles.
 * Used by both SuperManager and FleetManager controllers.
 */
public interface FleetManagementService {

    // Fleet Manager operations (SuperManager only)
    FleetManagerResponse registerFleetManager(CreateFleetManagerRequest request);
    FleetManagerResponse verifyFleetManager(UUID managerId);
    FleetManagerResponse updateFleetManager(UUID managerId, UpdateFleetManagerRequest request);
    void deleteFleetManager(UUID managerId);
    List<FleetManagerResponse> getAllFleetManagers();
    FleetManagerResponse getFleetManagerById(UUID managerId);

    // Driver operations (FleetManager)
    DriverResponse createDriver(UUID fleetManagerId, CreateDriverRequest request);
    DriverResponse updateDriver(UUID driverId, UpdateDriverRequest request);
    void deleteDriver(UUID driverId);
    List<DriverResponse> getDriversByManager(UUID fleetManagerId);
    DriverResponse getDriverById(UUID driverId);

    // Vehicle operations (FleetManager)
    VehicleResponse addVehicle(UUID fleetManagerId, AddVehicleRequest request);
    void deleteVehicle(UUID vehicleId);
    List<VehicleResponse> getVehiclesByManager(UUID fleetManagerId);

    // Dependent operations
    DependentResponse addDependent(CreateDependentRequest request);
    List<DependentResponse> getDependentsByDriver(UUID driverId);
}
