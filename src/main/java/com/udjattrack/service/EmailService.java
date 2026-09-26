package com.udjattrack.service;

public interface EmailService {

    void sendOtpEmail(String to, String otp, String name);

    void sendWelcomeEmail(String to, String name);

    void sendPasswordChangedEmail(String to, String name);

    void sendEmergencyAlert(String to, String driverName, String location);
    
    void sendLoginNotification(String to, String name, String deviceInfo);

    void sendRegistrationReceivedEmail(String to, String name);

    void sendAlertNotification(String to, String managerName, com.udjattrack.entity.Alert alert);
}
