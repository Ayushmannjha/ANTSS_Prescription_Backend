package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Entity
@Table(name="Patient")
@Setter
@Getter
@ToString
public class Patient {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
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
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
	
}
