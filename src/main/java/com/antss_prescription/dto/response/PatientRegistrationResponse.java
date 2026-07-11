package com.antss_prescription.dto.response;

import java.time.LocalDateTime;

import lombok.Data;
@Data
public class PatientRegistrationResponse {

	private int registrationId;
	private String registrationNumber;
	private int patientId;
	private String patientName;
	private String mobileNumber;
	private String gender;
	private String dateOfBirth;
	private int age;
	private String address;
	private String state;
	private String city;
	private String pincode;
	private Long clinicId;
	private String clinicName;
	private Long hospitalId;
	private String hospitalName;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
}
