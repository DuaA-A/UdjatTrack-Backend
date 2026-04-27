package com.udjattrack.service.impl;

import com.udjattrack.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * EmailServiceImpl — sends HTML emails via Spring Mail (SMTP).
 * All methods are @Async to avoid blocking the request thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${application.mail.from}")
    private String fromEmail;

    @Value("${application.mail.from-name}")
    private String fromName;

    @Override
    @Async("emailExecutor")
    public void sendOtpEmail(String to, String otp, String name) {
        String subject = "Verify Your Request — UdjatTrack Security Code";
        String html = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: auto; padding: 40px; border: 1px solid #e0e0e0; border-radius: 12px;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #1a3c5e; margin: 0; font-size: 28px;">UdjatTrack</h1>
                        <p style="color: #666; font-size: 14px; margin-top: 5px;">Secure Identity Verification</p>
                    </div>
                    <h2 style="color: #333; font-size: 20px;">Identity Verification Code</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>We received a request to access or modify your UdjatTrack account. Please use the following one-time password (OTP) to proceed. This code is valid for <strong>10 minutes</strong>.</p>
                    <div style="background: #f4f8ff; border-radius: 8px; padding: 30px; text-align: center; margin: 25px 0; border: 1px solid #d1e3ff;">
                        <span style="letter-spacing: 8px; color: #1a3c5e; font-size: 42px; font-weight: bold; font-family: monospace;">%s</span>
                    </div>
                    <p style="font-size: 13px; color: #777; background: #fff5f5; padding: 10px; border-radius: 4px;"><strong>Security Note:</strong> If you did not initiate this request, your account security may be at risk. Please contact our support team immediately.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;" />
                    <p style="font-size: 11px; color: #aaa; text-align: center;">This is an automated security notification. Please do not reply directly to this email.</p>
                </div>
                """.formatted(name, otp);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Welcome to UdjatTrack — Your Account is Ready!";
        String html = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; padding: 40px; border: 1px solid #e0e0e0; border-radius: 12px; color: #333;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #1a3c5e; margin: 0;">UdjatTrack</h1>
                        <p style="color: #666; font-size: 14px;">Professional Fleet Monitoring</p>
                    </div>
                    <h2 style="color: #1a3c5e;">Welcome, %s!</h2>
                    <p>Your UdjatTrack account has been successfully created. You can now access your dashboard to manage drivers, vehicles, and monitor live trips.</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="#" style="background-color: #1a3c5e; color: white; padding: 12px 25px; text-decoration: none; border-radius: 6px; font-weight: bold;">Login to Dashboard</a>
                    </div>
                    <p style="font-size: 14px; color: #666;">If you have any questions, our support team is here to help.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;" />
                    <p style="font-size: 12px; color: #aaa; text-align: center;">&copy; 2026 UdjatTrack. All rights reserved.</p>
                </div>
                """.formatted(name);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendPasswordChangedEmail(String to, String name) {
        String subject = "Security Alert: Password Changed Successfully";
        String html = """
                <div style="font-family: 'Segoe UI', Tahoma, sans-serif; max-width: 600px; margin: auto; padding: 40px; border: 1px solid #e0e0e0; border-radius: 12px;">
                    <h2 style="color: #1a3c5e;">Security Update</h2>
                    <p>Hi <strong>%s</strong>,</p>
                    <p>The password for your UdjatTrack account was recently changed. If this was you, you can safely ignore this email.</p>
                    <div style="background: #fff8f0; border-left: 4px solid #ff9800; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0; color: #856404;"><strong>Important:</strong> If you did not make this change, please contact our security team immediately to secure your account.</p>
                    </div>
                    <p style="font-size: 14px; color: #666;">Thank you for helping us keep your account safe.</p>
                </div>
                """.formatted(name);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendEmergencyAlert(String to, String driverName, String location) {
        String subject = "🚨 EMERGENCY: SOS Triggered";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 30px; border: 2px solid #d32f2f; border-radius: 12px;">
                    <div style="background-color: #d32f2f; color: white; padding: 10px; text-align: center; border-radius: 6px; margin-bottom: 20px;">
                        <h2 style="margin: 0;">URGENT SOS SIGNAL</h2>
                    </div>
                    <p>Driver <strong>%s</strong> has triggered a manual SOS alert during an active trip.</p>
                    <p><strong>Current Location:</strong> <span style="color: #d32f2f; font-weight: bold;">%s</span></p>
                    <p>Please initiate emergency protocols immediately.</p>
                    <hr/>
                    <p style="font-size: 12px; color: #777;">This is an automated emergency notification from the UdjatTrack Safety System.</p>
                </div>
                """.formatted(driverName, location);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendLoginNotification(String to, String name, String deviceInfo) {
        String subject = "Security Alert: New Login to Your Account";
        String html = """
                <div style="font-family: 'Segoe UI', Tahoma, sans-serif; max-width: 600px; margin: auto; padding: 40px; border: 1px solid #e0e0e0; border-radius: 12px;">
                    <div style="text-align: center; margin-bottom: 25px;">
                        <h2 style="color: #1a3c5e; margin: 0;">Account Login Alert</h2>
                    </div>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your UdjatTrack account was successfully accessed from a new device or browser. Below are the details of this login session:</p>
                    <div style="background: #f9f9f9; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #1a3c5e;">
                        <p style="margin: 5px 0;"><strong>Device/Client:</strong> %s</p>
                        <p style="margin: 5px 0;"><strong>Timestamp:</strong> %s</p>
                        <p style="margin: 5px 0;"><strong>Status:</strong> Authorized</p>
                    </div>
                    <p style="font-size: 14px; color: #555;">If this was you, you can safely ignore this notification. However, if you do not recognize this activity, we strongly recommend that you change your password and review your active sessions immediately to ensure your fleet data remains secure.</p>
                    <div style="text-align: center; margin-top: 30px;">
                        <a href="#" style="color: #1a3c5e; font-weight: bold; text-decoration: underline;">Review Account Security</a>
                    </div>
                </div>
                """.formatted(name, deviceInfo != null ? deviceInfo : "Standard Web/Mobile Browser", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm:ss")));
        sendHtmlEmail(to, subject, html);
    }

    // ---- Private helper ----

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.debug("Email sent to: {} | Subject: {}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
