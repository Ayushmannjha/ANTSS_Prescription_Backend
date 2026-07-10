package com.antss_prescription.dto.request;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class AppointmentRequestDto {

    private Long hospitalId;
    private String hospitalName;

    private Long doctorId;
    private String doctorName;

    private String patientName;
    private String mobileNumber;
    private Integer age;
    private String gender;

    private LocalDate appointmentDate;
    private LocalTime appointmentTime;

    private String reason;
}