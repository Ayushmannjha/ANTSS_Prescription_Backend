package com.antss_prescription.dto.response;

import com.antss_prescription.enums.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClinicResponse {
    private Long id;
    private UUID userId;
    private String clinicName;
    private String clinicCode;
    private String registrationNumber;
    private String addressLine1;
    private String city;
    private String state;
    private String pincode;
    private String email;
    private String mobileNumber;
    private Integer maxDoctorLimit;
    private Integer activeDoctorCount;
    private EntityStatus status;
}
