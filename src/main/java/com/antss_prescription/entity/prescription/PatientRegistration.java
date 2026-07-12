package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Hospital;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.Check;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "patient_registration")
@Check(constraints = "(hospital_id IS NOT NULL AND clinic_id IS NULL) OR (hospital_id IS NULL AND clinic_id IS NOT NULL)")
@Setter
@Getter
@ToString
public class PatientRegistration {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int registrationId;
	@Column(name = "registration_number", unique = true)
	@Size(max = 100)
	private String registrationNumber;//this is used as UHID
	@NotBlank
	@Size(max = 100)
	private String patientName;

	@Column(name = "mobile_number", nullable = true, length = 10)
	@Pattern(regexp = "^$|^[6-9][0-9]{9}$", message = "must be a valid 10-digit mobile number")
	private String mobileNumber;

	@NotBlank
	@Size(max = 20)
	private String gender;
	@NotBlank
	@Size(max = 20)
	private String dateOfBirth;
	@Min(0)
	@Max(150)
	private int age;
	@Size(max = 255)
	private String address;
	@Size(max = 100)
	private String state;
	@Size(max = 100)
	private String city;
	@Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "must be a valid 6-digit pincode")
	private String pincode;
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

	@AssertTrue(message = "Exactly one hospital or clinic is required")
	public boolean isFacilityExclusive() {
		return (hospital == null) != (clinic == null);
	}
}
