package com.antss_prescription.service.impl;

import com.antss_prescription.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {


    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendRegistrationNotificationToAdmin(String adminEmail, String userFullName,
                                                    String clinicName, String email,
                                                    String packageName, int numDoctors,
                                                    String approvalUrl) {
        String subject = "New Registration Pending Approval";
        
        String htmlContent = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Registration Pending Approval</title>\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; background-color: #0f172a; font-family: 'Outfit', 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #f8fafc; -webkit-font-smoothing: antialiased;\">\n" +
                "    <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"table-layout: fixed;\">\n" +
                "        <tr>\n" +
                "            <td align=\"center\" style=\"padding: 40px 10px;\">\n" +
                "                <!-- Card Container -->\n" +
                "                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 600px; background-color: #1e293b; border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 24px; box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3); overflow: hidden;\">\n" +
                "                    <!-- Header -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%); padding: 40px; text-align: center;\">\n" +
                "                            <div style=\"font-size: 14px; font-weight: 700; text-transform: uppercase; letter-spacing: 2px; color: #e9d5ff; margin-bottom: 8px;\">Antss Prescription</div>\n" +
                "                            <h1 style=\"margin: 0; font-size: 28px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px;\">New Registration Request</h1>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <!-- Content -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"padding: 40px;\">\n" +
                "                            <p style=\"margin-top: 0; margin-bottom: 24px; font-size: 16px; line-height: 1.6; color: #94a3b8; text-align: center;\">\n" +
                "                                A new partner has requested access to the Antss Prescription platform. Please review the details below.\n" +
                "                            </p>\n" +
                "                            \n" +
                "                            <!-- Details Box -->\n" +
                "                            <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #0f172a; border-radius: 16px; border: 1px solid rgba(255, 255, 255, 0.05); margin-bottom: 32px; overflow: hidden;\">\n" +
                "                                <tr>\n" +
                "                                    <td style=\"padding: 20px 24px; border-bottom: 1px solid rgba(255, 255, 255, 0.05);\">\n" +
                "                                        <div style=\"font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; color: #64748b; margin-bottom: 4px;\">Owner Full Name</div>\n" +
                "                                        <div style=\"font-size: 16px; font-weight: 700; color: #f1f5f9;\">" + userFullName + "</div>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                                <tr>\n" +
                "                                    <td style=\"padding: 20px 24px; border-bottom: 1px solid rgba(255, 255, 255, 0.05);\">\n" +
                "                                        <div style=\"font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; color: #64748b; margin-bottom: 4px;\">Entity Name</div>\n" +
                "                                        <div style=\"font-size: 16px; font-weight: 700; color: #f1f5f9;\">" + clinicName + "</div>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                                <tr>\n" +
                "                                    <td style=\"padding: 20px 24px; border-bottom: 1px solid rgba(255, 255, 255, 0.05);\">\n" +
                "                                        <div style=\"font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; color: #64748b; margin-bottom: 4px;\">Email Address</div>\n" +
                "                                        <div style=\"font-size: 16px; font-weight: 700; color: #38bdf8; font-family: monospace;\">" + email + "</div>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                                <tr>\n" +
                "                                    <td style=\"padding: 20px 24px; border-bottom: 1px solid rgba(255, 255, 255, 0.05);\">\n" +
                "                                        <div style=\"font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; color: #64748b; margin-bottom: 4px;\">Selected Plan</div>\n" +
                "                                        <div style=\"font-size: 16px; font-weight: 700; color: #e2e8f0; display: inline-block; background-color: rgba(99, 102, 241, 0.15); border: 1px solid rgba(99, 102, 241, 0.3); padding: 4px 10px; border-radius: 8px;\">" + packageName + "</div>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                                <tr>\n" +
                "                                    <td style=\"padding: 20px 24px;\">\n" +
                "                                        <div style=\"font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; color: #64748b; margin-bottom: 4px;\">Allowed Doctor Limit</div>\n" +
                "                                        <div style=\"font-size: 16px; font-weight: 700; color: #f1f5f9;\">" + numDoctors + " Doctors</div>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                            "                            </table>\n" +
                "                            \n" +
                "                            <!-- CTA Button -->\n" +
                "                            <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
                "                                <tr>\n" +
                "                                    <td align=\"center\">\n" +
                "                                        <a href=\"" + approvalUrl + "\" target=\"_blank\" style=\"display: inline-block; width: 80%; background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: #ffffff; text-decoration: none; font-size: 16px; font-weight: 700; text-align: center; padding: 16px 32px; border-radius: 12px; box-shadow: 0 10px 20px rgba(16, 185, 129, 0.25);\">\n" +
                "                                            Approve Registration\n" +
                "                                        </a>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </table>\n" +
                "                            \n" +
                "                            <p style=\"margin-top: 32px; margin-bottom: 0; font-size: 13px; line-height: 1.5; color: #64748b; text-align: center;\">\n" +
                "                                Clicking the button above will automatically approve this user, activate their subscription plan, and email their credentials securely.\n" +
                "                            </p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                    <!-- Footer -->\n" +
                "                    <tr>\n" +
                "                        <td style=\"background-color: #0f172a; padding: 24px; text-align: center; border-top: 1px solid rgba(255, 255, 255, 0.05);\">\n" +
                "                            <div style=\"font-size: 12px; color: #475569;\">&copy; 2026 Antss Prescription. All rights reserved.</div>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>";
        
        sendHtmlEmail(adminEmail, subject, htmlContent);
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
            throw new com.antss_prescription.exception.BusinessException("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("HTML Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            throw new com.antss_prescription.exception.BusinessException("Failed to send HTML email to " + to + ": " + e.getMessage());
        }
    }
}
