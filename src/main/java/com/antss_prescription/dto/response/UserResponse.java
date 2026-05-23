package com.antss_prescription.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import com.antss_prescription.enums.Role;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private UserType userType;
    private RegistrationStatus status;
    private Role role;
    private LocalDateTime registrationDate;
    private LocalDateTime createdAt;
}
