package com.antss_prescription.service;

public interface EmailService {
    void sendRegistrationNotificationToAdmin(String adminEmail, String userFullName,
                                             String clinicName, String email,
                                             String packageName, int numDoctors);
    void sendApprovalEmail(String toEmail, String fullName, String generatedPassword);
    void sendRejectionEmail(String toEmail, String fullName);
    void sendExpiryReminderEmail(String toEmail, String fullName);
    void sendPasswordResetEmail(String toEmail, String fullName, String resetToken);
    void sendCredentialsEmail(String toEmail, String entityName, String username, String plainPassword, String roleName, java.time.LocalDate endDate);
}
