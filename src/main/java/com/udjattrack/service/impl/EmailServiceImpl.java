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
        String subject = "UdjatTrack — Password Reset OTP";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 8px;">
                    <h2 style="color: #1a3c5e;">Password Reset Request</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>You requested a password reset. Use the OTP below. It expires in <strong>10 minutes</strong>.</p>
                    <div style="background: #f4f8ff; border: 2px dashed #1a3c5e; border-radius: 6px; padding: 20px; text-align: center; margin: 20px 0;">
                        <h1 style="letter-spacing: 10px; color: #1a3c5e; font-size: 36px; margin: 0;">%s</h1>
                    </div>
                    <p style="color: #888;">If you didn't request this, please ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
                    <p style="font-size: 12px; color: #aaa;">UdjatTrack Fleet Monitoring Platform</p>
                </div>
                """.formatted(name, otp);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Welcome to UdjatTrack!";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px;">
                    <h2 style="color: #1a3c5e;">Welcome, %s!</h2>
                    <p>Your UdjatTrack account has been created. You can now log in to manage your fleet.</p>
                    <p style="color: #888;">Best regards,<br/>The UdjatTrack Team</p>
                </div>
                """.formatted(name);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendPasswordChangedEmail(String to, String name) {
        String subject = "UdjatTrack — Password Changed Successfully";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px;">
                    <h2 style="color: #1a3c5e;">Password Updated</h2>
                    <p>Hi <strong>%s</strong>, your password was successfully changed.</p>
                    <p>If you did not make this change, please contact support immediately.</p>
                </div>
                """.formatted(name);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendEmergencyAlert(String to, String driverName, String location) {
        String subject = "🚨 UdjatTrack — Emergency Alert";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 2px solid #cc0000; border-radius: 8px;">
                    <h2 style="color: #cc0000;">⚠️ Emergency SOS Alert</h2>
                    <p>Driver <strong>%s</strong> has triggered an emergency SOS.</p>
                    <p><strong>Last Known Location:</strong> %s</p>
                    <p>Please respond immediately.</p>
                </div>
                """.formatted(driverName, location);
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
