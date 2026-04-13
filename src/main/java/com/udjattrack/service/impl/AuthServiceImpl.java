package com.udjattrack.service.impl;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.AuthResponse;
import com.udjattrack.dto.response.FleetManagerSignupResponse;
import com.udjattrack.entity.FleetManager;
import com.udjattrack.entity.OtpToken;
import com.udjattrack.entity.RefreshToken;
import com.udjattrack.entity.User;
import com.udjattrack.entity.enums.VerificationStatus;
import com.udjattrack.exception.BusinessException;
import com.udjattrack.exception.InvalidOtpException;
import com.udjattrack.exception.ResourceNotFoundException;
import com.udjattrack.repository.FleetManagerRepository;
import com.udjattrack.repository.OtpTokenRepository;
import com.udjattrack.repository.RefreshTokenRepository;
import com.udjattrack.repository.UserRepository;
import com.udjattrack.security.JwtUtil;
import com.udjattrack.service.AuthService;
import com.udjattrack.service.EmailService;
import com.udjattrack.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.udjattrack.entity.enums.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;
import com.udjattrack.entity.FleetManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final FleetManagerRepository fleetManagerRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final OtpUtil otpUtil;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    @Value("${application.otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Spring Security validates credentials (throws on failure)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.email()));

        String accessToken = jwtUtil.generateToken(
                user.getEmail(), user.getRole().name(), user.getUserId().toString());

        String refreshTokenValue = UUID.randomUUID().toString();
        saveRefreshToken(user, refreshTokenValue, request.deviceInfo());

        log.info("User logged in: {} [{}]", user.getEmail(), user.getRole());

        return buildAuthResponse(user, accessToken, refreshTokenValue);
    }

    // =====================================================================
    // Logout
    // =====================================================================

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                    log.info("User logged out — refresh token revoked");
                });
    }

    // =====================================================================
    // Forgot Password — Step 1: Request OTP
    // =====================================================================

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + request.email()));

        // Invalidate any previous OTPs for this email
        otpTokenRepository.invalidateAllForEmail(request.email());

        String code = otpUtil.generateOtp();
        OtpToken otp = OtpToken.builder()
                .email(request.email())
                .otpCode(code)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .used(false)
                .build();
        otpTokenRepository.save(otp);

        emailService.sendOtpEmail(request.email(), code, user.getName());
        log.info("OTP sent to: {}", request.email());
    }

    // =====================================================================
    // Forgot Password — Step 2: Verify OTP
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public void verifyOtp(VerifyOtpRequest request) {
        OtpToken otp = otpTokenRepository
                .findByEmailAndOtpCodeAndUsedFalse(request.email(), request.otpCode())
                .orElseThrow(() -> new InvalidOtpException("Invalid OTP code"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("OTP has expired. Please request a new one.");
        }
        // Just validation — OTP is consumed in resetPassword
    }

    // =====================================================================
    // Forgot Password — Step 3: Reset Password
    // =====================================================================

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OtpToken otp = otpTokenRepository
                .findByEmailAndOtpCodeAndUsedFalse(request.email(), request.otpCode())
                .orElseThrow(() -> new InvalidOtpException("Invalid or already used OTP"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("OTP has expired. Please request a new one.");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.email()));

        // Update password
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Mark OTP as used
        otp.setUsed(true);
        otpTokenRepository.save(otp);

        // Revoke all sessions — force re-login with new password
        refreshTokenRepository.revokeAllByUserId(user.getUserId());

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getName());
        log.info("Password reset for: {}", user.getEmail());
    }

    // =====================================================================
    // Token Refresh
    // =====================================================================

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> new BusinessException("Invalid or revoked refresh token"));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new BusinessException("Refresh token has expired. Please login again.");
        }

        User user = storedToken.getUser();

        // Rotate: revoke old, issue new
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtUtil.generateToken(
                user.getEmail(), user.getRole().name(), user.getUserId().toString());
        String newRefreshToken = UUID.randomUUID().toString();
        saveRefreshToken(user, newRefreshToken, storedToken.getDeviceInfo());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    // =====================================================================
    // Private Helpers
    // =====================================================================

    private void saveRefreshToken(User user, String tokenValue, String deviceInfo) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .deviceInfo(deviceInfo)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .userId(user.getUserId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public FleetManagerSignupResponse signupFleetManager(CreateFleetManagerRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered: " + request.email());
        }
        FleetManager manager = FleetManager.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.ROLE_FLEET_MANAGER)
                .companyName(request.companyName())
                .subscriptionPlan(request.subscriptionPlan())
                .verificationStatus(VerificationStatus.PENDING)
                .isDeleted(false)
                .build();
        FleetManager saved = fleetManagerRepository.save(manager);
        log.info("Fleet manager registered: {} — awaiting approval", saved.getEmail());
        return FleetManagerSignupResponse.builder()
                .fleetManagerId(saved.getUserId())
                .status("PENDING_APPROVAL")
                .build();
    }

    @Override
    @Transactional
    public FleetManagerSignupResponse approveFleetManager(UUID managerId) {
        FleetManager manager = fleetManagerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetManager", "id", managerId.toString()));
        manager.setVerificationStatus(VerificationStatus.VERIFIED);
        fleetManagerRepository.save(manager);
        emailService.sendWelcomeEmail(manager.getEmail(), manager.getName());
        log.info("Fleet manager approved: {}", manager.getEmail());
        return FleetManagerSignupResponse.builder()
                .fleetManagerId(manager.getUserId())
                .status("ACTIVE")
                .build();
    }

    @Override
    @Transactional
    public FleetManagerSignupResponse rejectFleetManager(UUID managerId, String reason) {
        FleetManager manager = fleetManagerRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("FleetManager", "id", managerId.toString()));
        manager.setVerificationStatus(VerificationStatus.REJECTED);
        fleetManagerRepository.save(manager);
        log.info("Fleet manager rejected: {} — reason: {}", manager.getEmail(), reason);
        return FleetManagerSignupResponse.builder()
                .fleetManagerId(manager.getUserId())
                .status("REJECTED")
                .build();
    }
}
