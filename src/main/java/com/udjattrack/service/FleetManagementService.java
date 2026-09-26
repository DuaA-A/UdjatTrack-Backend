package com.udjattrack.service;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FleetManagementService {
    FleetManagerResponse registerFleetManager(CreateFleetManagerRequest request);
    FleetManagerResponse verifyFleetManager(UUID managerId);
    FleetManagerResponse updateFleetManager(UUID managerId, UpdateFleetManagerRequest request);
    void deleteFleetManager(UUID managerId);
    List<FleetManagerResponse> getAllFleetManagers();
    FleetManagerResponse getFleetManagerById(UUID managerId);

    DriverResponse createDriver(UUID fleetManagerId, CreateDriverRequest request);
    DriverResponse updateDriver(UUID driverId, UpdateDriverRequest request);
    void deleteDriver(UUID driverId);
    List<DriverResponse> getDriversByManager(UUID fleetManagerId);
    DriverResponse getDriverById(UUID driverId);
    DriverResponse uploadDriverPhoto(UUID driverId, MultipartFile file);
    VehicleResponse addVehicle(UUID fleetManagerId, AddVehicleRequest request);
    void deleteVehicle(UUID vehicleId);
    List<VehicleResponse> getVehiclesByManager(UUID fleetManagerId, String plateNumber);
    VehicleResponse getVehicleById(UUID vehicleId);
    VehicleWithDriverResponse getVehicleWithDriver(UUID vehicleId);
    DependentResponse addDependent(UUID driverId, CreateDependentRequest request);
    List<DependentResponse> getDependentsByDriver(UUID driverId);
}
