package com.udjattrack.service;

/**
 * Email service — sends system emails (OTP, notifications, emergency alerts).
 */
public interface EmailService {

    void sendOtpEmail(String to, String otp, String name);

    void sendWelcomeEmail(String to, String name);

    void sendPasswordChangedEmail(String to, String name);

    void sendEmergencyAlert(String to, String driverName, String location);
    
    void sendLoginNotification(String to, String name, String deviceInfo);
}
