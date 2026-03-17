package com.udjattrack.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "OTP code is required")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        String otpCode
) {}
