package com.antss_prescription.service;

import com.antss_prescription.enums.UserType;

public interface EmailService {
    void sendRegistrationNotificationToAdmin(String adminEmail, String userFullName,
                                             String clinicName, String email,
                                             String packageName, int numDoctors,
                                             String approvalUrl);
    void sendApprovalEmail(String toEmail, String fullName, String resetToken);
    void sendRejectionEmail(String toEmail, String fullName);
    void sendExpiryReminderEmail(String toEmail, String fullName);
    void sendPasswordResetEmail(String toEmail, String fullName, String resetToken, UserType userType);
    void sendCredentialsEmail(String toEmail, String entityName, String username, String roleName,
                              java.time.LocalDate endDate, UserType userType, String resetToken);
}
