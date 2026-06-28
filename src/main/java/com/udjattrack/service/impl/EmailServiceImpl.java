package com.udjattrack.service.impl;

import com.udjattrack.entity.Alert;
import com.udjattrack.entity.enums.SeverityLevel;
import com.udjattrack.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${application.mail.from}")
    private String fromEmail;

    @Value("${application.mail.from-name}")
    private String fromName;

    @Value("${spring.mail.host:Not Configured}")
    private String smtpHost;

    @Value("${spring.mail.port:Not Configured}")
    private String smtpPort;

    @Value("${spring.mail.username:Not Configured}")
    private String smtpUsername;

    @Value("${spring.mail.password:Not Configured}")
    private String smtpPassword;

    @PostConstruct
    private void validateMailConfiguration() {
        log.info("Mail configuration loaded: host={}, port={}, username={}, from={}, fromName={}",
                smtpHost, smtpPort, smtpUsername, fromEmail, fromName);

        if (!StringUtils.hasText(smtpHost)
                || !StringUtils.hasText(smtpPort)
                || !StringUtils.hasText(smtpUsername)
                || !StringUtils.hasText(smtpPassword)
                || !StringUtils.hasText(fromEmail)
                || !StringUtils.hasText(fromName)) {
            log.error("Mail configuration incomplete. Set MAIL_HOST, MAIL_PORT, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM, MAIL_FROM_NAME in Railway environment variables.");
        }
    }

    @Override
    @Async("emailExecutor")
    public void sendOtpEmail(String to, String otp, String name) {
        String subject = "Verify Your Request — UdjatTrack Security Code";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd;">
                    <h2>Verification Code</h2>
                    <p>Hi %s,</p>
                    <p>Your security code is:</p>
                    <div style="background: #f4f4f4; padding: 20px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px;">
                        %s
                    </div>
                    <p>This code expires in 10 minutes.</p>
                </div>
                """.formatted(name, otp);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Welcome to UdjatTrack!";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd;">
                    <h2>Welcome, %s!</h2>
                    <p>Your account has been successfully created. You can now start managing your fleet.</p>
                </div>
                """.formatted(name);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendPasswordChangedEmail(String to, String name) {
        String subject = "Security Alert: Password Changed";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd;">
                    <h2>Password Changed</h2>
                    <p>Hi %s, your password was recently changed. If this wasn't you, please contact support.</p>
                </div>
                """.formatted(name);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendEmergencyAlert(String to, String driverName, String location) {
        String subject = "🚨 EMERGENCY: SOS Triggered";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 2px solid red;">
                    <h2 style="color: red;">URGENT SOS SIGNAL</h2>
                    <p>Driver <strong>%s</strong> has triggered an SOS alert.</p>
                    <p><strong>Location:</strong> %s</p>
                </div>
                """.formatted(driverName, location);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendLoginNotification(String to, String name, String deviceInfo) {
        String subject = "Welcome Back to UdjatTrack!";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd;">
                    <h2>Welcome Back, %s!</h2>
                    <p>You have successfully logged into your account.</p>
                    <p style="color: #666; font-size: 12px;">Logged in via: %s</p>
                </div>
                """.formatted(name, deviceInfo != null ? deviceInfo : "your device");
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendAlertNotification(String to, String managerName, Alert alert) {
        String severityEmoji = alert.getSeverity() == SeverityLevel.CRITICAL ? "🚨 CRITICAL" : "⚠️ ALERT";
        String subject = severityEmoji + ": " + alert.getAlertType() + " Detected";
        
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd;">
                    <h2>Fleet Alert Notification</h2>
                    <p>Hi %s, an alert was triggered:</p>
                    <ul>
                        <li><strong>Type:</strong> %s</li>
                        <li><strong>Severity:</strong> %s</li>
                        <li><strong>Message:</strong> %s</li>
                        <li><strong>Driver:</strong> %s</li>
                    </ul>
                </div>
                """.formatted(managerName, alert.getAlertType(), alert.getSeverity(), alert.getMessage(), alert.getTrip().getDriver().getName());
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async("emailExecutor")
    public void sendRegistrationReceivedEmail(String to, String name) {
        String subject = "Registration Received — UdjatTrack";
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd;">
                    <h2>Registration Received</h2>
                    <p>Hi %s,</p>
                    <p>Thank you for registering with UdjatTrack. Your account is currently <strong>awaiting approval</strong> from our administrators.</p>
                    <p>You will receive another email once your account has been verified.</p>
                </div>
                """.formatted(name);
        sendHtmlEmail(to, subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        log.info("[EMAIL TRACE] Sending email to={} via {}:{} from={}", to, smtpHost, smtpPort, fromEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("[EMAIL TRACE] SUCCESS: Email sent successfully to {} | Subject: {}", to, subject);
        } catch (Exception e) {
            log.error("[EMAIL TRACE] ERROR: Failed to send email to {}. Reason: {}", to, e.getMessage(), e);
        }
    }
}
