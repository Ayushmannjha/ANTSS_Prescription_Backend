package com.antss_prescription.service.impl;

import com.antss_prescription.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {


    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendRegistrationNotificationToAdmin(String adminEmail, String userFullName,
                                                    String clinicName, String email,
                                                    String packageName, int numDoctors) {
        String subject = "New Registration Pending Approval";
        String body = "A new user has registered and requires your approval.\n\n" +
                "Name: " + userFullName + "\n" +
                "Clinic: " + clinicName + "\n" +
                "Email: " + email + "\n" +
                "Package: " + packageName + "\n" +
                "Doctors Required: " + numDoctors + "\n\n" +
                "Please log in to the admin panel to review this registration.";
        sendEmail(adminEmail, subject, body);
    }

    @Override
    public void sendApprovalEmail(String toEmail, String fullName, String generatedPassword) {
        String subject = "Account Approved - Antss Prescription";
        String body = "Dear " + fullName + ",\n\n" +
                "Your account has been approved. You can now log in to the Antss Prescription portal.\n\n" +
                "Your Login Credentials:\n" +
                "Username: " + toEmail + "\n" +
                "Password: " + generatedPassword + "\n\n" +
                "Please change your password after logging in for security purposes.\n\n" +
                "Thank you for choosing Antss Prescription.";
        sendEmail(toEmail, subject, body);
    }

    @Override
    public void sendRejectionEmail(String toEmail, String fullName) {
        String subject = "Account Registration - Update";
        String body = "Dear " + fullName + ",\n\n" +
                "We regret to inform you that your account registration has not been approved at this time.\n\n" +
                "Please contact our support team for more information.";
        sendEmail(toEmail, subject, body);
    }

    @Override
    public void sendExpiryReminderEmail(String toEmail, String fullName) {
        String subject = "Subscription Expired - Antss Prescription";
        String body = "Dear " + fullName + ",\n\n" +
                "Your Antss Prescription subscription has expired. " +
                "Please contact our support team to renew your subscription and restore access.\n\n" +
                "Thank you.";
        sendEmail(toEmail, subject, body);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        String subject = "Password Reset Request - Antss Prescription";
        String body = "Dear " + fullName + ",\n\n" +
                "We received a request to reset your password.\n\n" +
                "Your password reset token is:\n" + resetToken + "\n\n" +
                "This token is valid for 1 hour. If you did not request a password reset, please ignore this email.";
        sendEmail(toEmail, subject, body);
    }

    @Override
    public void sendCredentialsEmail(String toEmail, String entityName, String username, String plainPassword, String roleName, java.time.LocalDate endDate) {
        String subject = "Access Credentials - Antss Prescription";
        String body = "Dear " + entityName + ",\n\n" +
                "You have been added to the Antss Prescription portal as a " + roleName + ".\n\n" +
                "Your Access Credentials:\n" +
                "Username/Email: " + username + "\n" +
                "Password: " + plainPassword + "\n\n" +
                "Your subscription / validity is active until: " + endDate + "\n\n" +
                "Please use these credentials to log in to the Antss portal.\n\n" +
                "Thank you.";
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
