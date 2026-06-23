package com.antss_prescription.docs.service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDto {

    private Integer id;
    private String fileName;
    private String url;
    private String documentType;

    private int patientId;
    private String patientName;
    private String mobileNumber;
}
