package com.antss_prescription.dto.response;

import com.antss_prescription.entity.prescription.Patient;

import lombok.Data;
@Data
public class PatientRegistrationResponse {

	private int registrationId;
	private String registrationNumber;
	private Patient patient;
	private Long clinicId;
	private String clinicName;
	private Long hospitalId;
	private String hospitalName;
	
}
