package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="Patient")
@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
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


	@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Document> docs;

}
