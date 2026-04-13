package com.udjattrack.controller;

import com.udjattrack.dto.request.*;
import com.udjattrack.dto.response.ApiResponse;
import com.udjattrack.dto.response.AuthResponse;
import com.udjattrack.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.udjattrack.dto.request.RejectFleetManagerRequest;
import com.udjattrack.dto.response.FleetManagerSignupResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, forgot password, OTP, and token refresh")
public class AuthController {

    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Returns JWT access token + refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the provided refresh token (server-side logout)")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Sends OTP to the registered email address")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("OTP sent to " + request.email() + ". Valid for 10 minutes.")
                        .build());
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Validates OTP before allowing password reset")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("OTP verified successfully")
                .build());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset Password", description = "Resets password and invalidates all active sessions")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Password reset successfully. Please login with your new password.")
                .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Issues new access + refresh token pair (token rotation)")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", response));
    }

    @PostMapping("/signup/fleet-manager")
    @PreAuthorize("hasAuthority('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Register Fleet Manager")
    public ResponseEntity<ApiResponse<FleetManagerSignupResponse>> signupFleetManager(
            @Valid @RequestBody CreateFleetManagerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Fleet manager registered. Pending approval.",
                        authService.signupFleetManager(request)));
    }

    @PatchMapping("/fleet-managers/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Approve Fleet Manager")
    public ResponseEntity<ApiResponse<FleetManagerSignupResponse>> approveFleetManager(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Fleet manager approved.",
                authService.approveFleetManager(id)));
    }

    @PatchMapping("/fleet-managers/{id}/reject")
    @PreAuthorize("hasAuthority('ROLE_SUPER_MANAGER')")
    @Operation(summary = "Reject Fleet Manager")
    public ResponseEntity<ApiResponse<FleetManagerSignupResponse>> rejectFleetManager(
            @PathVariable UUID id,
            @Valid @RequestBody RejectFleetManagerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Fleet manager rejected.",
                authService.rejectFleetManager(id, request.reason())));
    }


    @GetMapping("/dev/hash")
    public ResponseEntity<String> generateHash(@RequestParam String raw) {
        return ResponseEntity.ok(passwordEncoder.encode(raw));
    }
}
