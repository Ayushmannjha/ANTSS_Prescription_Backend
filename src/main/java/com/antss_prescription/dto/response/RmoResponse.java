package com.antss_prescription.dto.response;

import com.antss_prescription.enums.EntityStatus;
import com.antss_prescription.enums.RmoRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RmoResponse {
    private UUID id;
    private UUID userId;
    private String rmoName;
    private String email;
    private String mobileNumber;
    private String employeeCode;
    private Long hospitalId;
    private Long clinicId;
    private RmoRole role;
    private EntityStatus status;
}
