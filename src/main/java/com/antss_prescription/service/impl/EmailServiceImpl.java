package com.antss_prescription.service.impl;

import com.antss_prescription.service.EmailService;
import com.antss_prescription.enums.UserType;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {


    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.owner-frontend-url}")
    private String ownerFrontendUrl;

    @Value("${app.prescription-frontend-url}")
    private String prescriptionFrontendUrl;

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
    public void sendApprovalEmail(String toEmail, String fullName, String resetToken) {
        String subject = "Account Approved - Antss Prescription";
        String passwordSetupUrl = buildOwnerFrontendUrl("/login?resetToken=" + encode(resetToken));
        String htmlContent = buildActionEmail(
                "Account Approved",
                "Dear " + fullName + ", your account has been approved. Use the button below to create your password.",
                "Username: " + toEmail + "<br>This secure setup link is valid for 1 hour.",
                "Set Password",
                passwordSetupUrl
        );
        sendHtmlEmail(toEmail, subject, htmlContent);
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
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken, UserType userType) {
        String subject = "Password Reset Request - Antss Prescription";
        String resetUrl = buildFrontendUrlForUserType(userType, "/login?resetToken=" + encode(resetToken));
        String htmlContent = buildActionEmail(
                "Reset Your Password",
                "Dear " + fullName + ", we received a request to reset your password.",
                "Use the button below to open the reset password page. This link is valid for 1 hour. If you did not request this, please ignore this email.",
                "Reset Password",
                resetUrl
        );
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendCredentialsEmail(String toEmail, String entityName, String username, String roleName,
                                     java.time.LocalDate endDate, UserType userType, String resetToken) {
        String subject = "Access Credentials - Antss Prescription";
        String setupUrl = buildFrontendUrlForUserType(userType, "/login?resetToken=" + encode(resetToken));
        String htmlContent = buildActionEmail(
                "Portal Access Created",
                "Dear " + entityName + ", you have been added to the Antss Prescription portal as a " + roleName + ".",
                "Username/Email: " + username + "<br>Your subscription / validity is active until: " + endDate + "<br>Use the button below to create your password.",
                "Set Password",
                setupUrl
        );
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    private String buildActionEmail(String title, String intro, String details, String buttonText, String buttonUrl) {
        return "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "  <meta charset=\"UTF-8\">" +
                "  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "  <title>" + title + " - Antss Prescription</title>" +
                "</head>" +
                "<body style=\"margin:0;padding:0;background:#0f172a;font-family:Arial,Helvetica,sans-serif;color:#f8fafc;\">" +
                "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#0f172a;padding:40px 12px;\">" +
                "    <tr><td align=\"center\">" +
                "      <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;background:#1e293b;border:1px solid rgba(255,255,255,0.08);border-radius:20px;overflow:hidden;\">" +
                "        <tr><td style=\"background:#f97316;padding:28px 32px;text-align:center;\">" +
                "          <div style=\"font-size:12px;font-weight:700;letter-spacing:2px;text-transform:uppercase;color:#fff7ed;\">Antss Prescription</div>" +
                "          <h1 style=\"margin:8px 0 0;font-size:26px;line-height:1.25;color:#ffffff;\">" + title + "</h1>" +
                "        </td></tr>" +
                "        <tr><td style=\"padding:32px;\">" +
                "          <p style=\"margin:0 0 18px;font-size:16px;line-height:1.6;color:#cbd5e1;\">" + intro + "</p>" +
                "          <div style=\"margin:0 0 28px;padding:16px 18px;background:#0f172a;border:1px solid rgba(255,255,255,0.06);border-radius:12px;font-size:14px;line-height:1.7;color:#e2e8f0;\">" + details + "</div>" +
                "          <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\">" +
                "            <a href=\"" + buttonUrl + "\" target=\"_blank\" style=\"display:inline-block;background:#ea580c;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;padding:14px 28px;border-radius:10px;\">" + buttonText + "</a>" +
                "          </td></tr></table>" +
                "          <p style=\"margin:28px 0 0;font-size:12px;line-height:1.6;color:#64748b;text-align:center;\">If the button does not open, please contact support.</p>" +
                "        </td></tr>" +
                "      </table>" +
                "    </td></tr>" +
                "  </table>" +
                "</body></html>";
    }

    private String buildOwnerFrontendUrl(String path) {
        return buildFrontendUrl(ownerFrontendUrl, path);
    }

    private String buildPrescriptionFrontendUrl(String path) {
        return buildFrontendUrl(prescriptionFrontendUrl, path);
    }

    private String buildFrontendUrlForUserType(UserType userType, String path) {
        if (userType == UserType.DOCTOR || userType == UserType.RMO) {
            return buildPrescriptionFrontendUrl(path);
        }
        return buildOwnerFrontendUrl(path);
    }

    private String buildFrontendUrl(String frontendUrl, String path) {
        String normalizedBase = frontendUrl == null ? "" : frontendUrl.replaceAll("/+$", "");
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
            throw new com.antss_prescription.exception.BusinessException("Unable to send email at this time");
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
            throw new com.antss_prescription.exception.BusinessException("Unable to send email at this time");
        }
    }
}
