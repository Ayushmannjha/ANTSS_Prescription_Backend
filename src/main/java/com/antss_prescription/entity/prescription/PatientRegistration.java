package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Hospital;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "patient_registration")
@Setter
@Getter
@ToString
public class PatientRegistration {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int registrationId;
	@Size(max = 100)
	private String registrationNumber;//this is used as UHID
	@ManyToOne
	@JoinColumn(name = "patient_id")
	@NotNull
	@Valid
	private Patient patient;
	@ManyToOne
	@JoinColumn(name="clinic_id", nullable = true)
	private Clinic clinic;
	@ManyToOne
	@JoinColumn(name="hospital_id", nullable = true)
	private Hospital hospital;
	@Size(max = 50)
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
