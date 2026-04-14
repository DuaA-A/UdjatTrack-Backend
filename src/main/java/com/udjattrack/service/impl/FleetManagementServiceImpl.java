package com.udjattrack.service.impl;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.*;
import com.udjattrack.entity.*;
import com.udjattrack.entity.enums.UserRole;
import com.udjattrack.entity.enums.VerificationStatus;
import com.udjattrack.exception.BusinessException;
import com.udjattrack.exception.DuplicateResourceException;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.*;
import com.udjattrack.service.EmailService;
import com.udjattrack.service.FleetManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FleetManagementServiceImpl implements FleetManagementService {

    private final FleetManagerRepository fleetManagerRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final DependentRepository dependentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ===== Fleet Manager =====

    @Override
    public FleetManagerResponse registerFleetManager(CreateFleetManagerRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("FleetManager", "email", request.email());
        }
        FleetManager manager = FleetManager.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .companyName(request.companyName())
                .subscriptionPlan(request.subscriptionPlan())
                .verificationStatus(VerificationStatus.PENDING)
                .role(UserRole.ROLE_FLEET_MANAGER)
                .isDeleted(false)
                .isRead(false)
                .build();
        FleetManager saved = fleetManagerRepository.save(manager);
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());
        return toResponse(saved);
    }

    @Override
    public FleetManagerResponse verifyFleetManager(UUID managerId) {
        FleetManager manager = findManagerOrThrow(managerId);
        if (manager.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new BusinessException("Fleet manager is already verified");
        }
        manager.setVerificationStatus(VerificationStatus.VERIFIED);
        return toResponse(fleetManagerRepository.save(manager));
    }

    @Override
    public FleetManagerResponse updateFleetManager(UUID managerId, UpdateFleetManagerRequest request) {
        FleetManager manager = findManagerOrThrow(managerId);
        if (request.name() != null) manager.setName(request.name());
        if (request.companyName() != null) manager.setCompanyName(request.companyName());
        if (request.subscriptionPlan() != null) manager.setSubscriptionPlan(request.subscriptionPlan());
        return toResponse(fleetManagerRepository.save(manager));
    }

    @Override
    public void deleteFleetManager(UUID managerId) {
        FleetManager manager = findManagerOrThrow(managerId);
        manager.setIsDeleted(true);
        manager.setDeletedAt(LocalDateTime.now());
        fleetManagerRepository.save(manager);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FleetManagerResponse> getAllFleetManagers() {
        return fleetManagerRepository.findAllByIsDeletedFalse()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FleetManagerResponse getFleetManagerById(UUID managerId) {
        return toResponse(findManagerOrThrow(managerId));
    }

    // ===== Drivers =====

    @Override
    public DriverResponse createDriver(UUID fleetManagerId, CreateDriverRequest request) {
        FleetManager manager = findManagerOrThrow(fleetManagerId);
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Driver", "email", request.email());
        }
        if (driverRepository.existsByLicenseNumberAndIsDeletedFalse(request.licenseNumber())) {
            throw new DuplicateResourceException("Driver", "licenseNumber", request.licenseNumber());
        }
        Driver driver = Driver.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .licenseNumber(request.licenseNumber())
                .phoneNumber(request.phoneNumber())
                .idle(true)
                .fleetManager(manager)
                .role(UserRole.ROLE_DRIVER)
                .isDeleted(false)
                .isRead(false)
                .build();
        Driver saved = driverRepository.save(driver);
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());
        return toDriverResponse(saved);
    }

    @Override
    public DriverResponse updateDriver(UUID driverId, UpdateDriverRequest request) {
        Driver driver = findDriverOrThrow(driverId);
        if (request.name() != null) driver.setName(request.name());
        if (request.phoneNumber() != null) driver.setPhoneNumber(request.phoneNumber());
        if (request.licenseNumber() != null) driver.setLicenseNumber(request.licenseNumber());
        return toDriverResponse(driverRepository.save(driver));
    }

    @Override
    public void deleteDriver(UUID driverId) {
        Driver driver = findDriverOrThrow(driverId);
        driver.setIsDeleted(true);
        driver.setDeletedAt(LocalDateTime.now());
        driverRepository.save(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getDriversByManager(UUID fleetManagerId) {
        return driverRepository.findActiveDriversByManager(fleetManagerId)
                .stream().map(this::toDriverResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getDriverById(UUID driverId) {
        return toDriverResponse(findDriverOrThrow(driverId));
    }

    // ===== Vehicles =====

    @Override
    public VehicleResponse addVehicle(UUID fleetManagerId, AddVehicleRequest request) {
        FleetManager manager = findManagerOrThrow(fleetManagerId);
        if (vehicleRepository.existsByPlateNumberAndIsDeletedFalse(request.plateNumber())) {
            throw new DuplicateResourceException("Vehicle", "plateNumber", request.plateNumber());
        }
        Vehicle vehicle = Vehicle.builder()
                .plateNumber(request.plateNumber())
                .model(request.model())
                .manufactureYear(request.manufactureYear())
                .idle(true)
                .working(true)
                .isDeleted(false)
                .fleetManager(manager)
                .build();
        return toVehicleResponse(vehicleRepository.save(vehicle));
    }

    @Override
    public void deleteVehicle(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));
        vehicle.setIsDeleted(true);
        vehicle.setDeletedAt(LocalDateTime.now());
        vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByManager(UUID fleetManagerId) {
        return vehicleRepository.findAllByFleetManagerUserIdAndIsDeletedFalse(fleetManagerId)
                .stream().map(this::toVehicleResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", vehicleId));
        return toVehicleResponse(vehicle);
    }

    @Override
    public DependentResponse addDependent(CreateDependentRequest request) {
        Driver driver = findDriverOrThrow(request.driverId());
        Dependent dep = Dependent.builder()
                .driver(driver)
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .relation(request.relation())
                .build();
        Dependent saved = dependentRepository.save(dep);
        return toDependentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DependentResponse> getDependentsByDriver(UUID driverId) {
        return dependentRepository.findAllByDriverUserId(driverId)
                .stream().map(this::toDependentResponse).collect(Collectors.toList());
    }

    // ===== Private helpers =====

    private FleetManager findManagerOrThrow(UUID id) {
        return fleetManagerRepository.findById(id)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("FleetManager", "id", id));
    }

    private Driver findDriverOrThrow(UUID id) {
        return driverRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", id));
    }

    private FleetManagerResponse toResponse(FleetManager m) {
        return FleetManagerResponse.builder()
                .userId(m.getUserId()).name(m.getName()).email(m.getEmail())
                .companyName(m.getCompanyName()).subscriptionPlan(m.getSubscriptionPlan())
                .verificationStatus(m.getVerificationStatus()).createdAt(m.getCreatedAt())
                .build();
    }

    private DriverResponse toDriverResponse(Driver d) {
        return DriverResponse.builder()
                .userId(d.getUserId()).name(d.getName()).email(d.getEmail())
                .licenseNumber(d.getLicenseNumber()).phoneNumber(d.getPhoneNumber())
                .idle(d.getIdle()).fleetManagerId(d.getFleetManager().getUserId())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private VehicleResponse toVehicleResponse(Vehicle v) {
        return VehicleResponse.builder()
                .vehicleId(v.getVehicleId()).plateNumber(v.getPlateNumber())
                .model(v.getModel()).manufactureYear(v.getManufactureYear())
                .idle(v.getIdle()).working(v.getWorking())
                .fleetManagerId(v.getFleetManager().getUserId()).createdAt(v.getCreatedAt())
                .build();
    }

    private DependentResponse toDependentResponse(Dependent d) {
        return DependentResponse.builder()
                .dependentId(d.getDependentId()).driverId(d.getDriver().getUserId())
                .name(d.getName()).phoneNumber(d.getPhoneNumber()).relation(d.getRelation())
                .build();
    }
}
