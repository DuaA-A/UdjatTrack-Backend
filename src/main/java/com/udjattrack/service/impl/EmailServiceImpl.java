package com.udjattrack.service.impl;

import com.udjattrack.entity.Alert;
import com.udjattrack.entity.enums.SeverityLevel;
import com.udjattrack.service.EmailService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${application.mail.resend.api-key:re_ZvKJgNMD_J6JtGfLo3oXtreX1Yww67Gy6}")
    private String resendApiKey;

    // Using the newly purchased domain!
    private final String fromEmail = "no-reply@udjattrack.online";

    @Value("${application.mail.from-name:UdjatTrack}")
    private String fromName;

    private Resend resend;

    @PostConstruct
    public void setupResend() {
        log.info("Setting up Resend API using custom domain. from={}, fromName={}", fromEmail, fromName);

        if (!StringUtils.hasText(resendApiKey) || !StringUtils.hasText(fromName)) {
            log.error("Mail configuration incomplete. Check resend-api-key and from-name.");
        }
        
        this.resend = new Resend(resendApiKey);
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
        String fromString = fromName + " <" + fromEmail + ">";
        log.info("[EMAIL TRACE] Sending email to={} via Resend from={}", to, fromString);

        try {
            CreateEmailOptions sendEmailRequest = CreateEmailOptions.builder()
                    .from(fromString)
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse data = resend.emails().send(sendEmailRequest);
            log.info("[EMAIL TRACE] SUCCESS: Email sent successfully to {} | Subject: {} | Resend ID: {}", to, subject, data.getId());
        } catch (ResendException e) {
            log.error("[EMAIL TRACE] ERROR: Failed to send email to {} via Resend. Reason: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            log.error("[EMAIL TRACE] ERROR: Unexpected error while sending email to {}. Reason: {}", to, e.getMessage(), e);
        }
    }
}
