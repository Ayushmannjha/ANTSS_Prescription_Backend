package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import jakarta.annotation.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.ManyToOne;

@Entity
@Table(name="past_medical_history")
@Setter
@Getter
@ToString(exclude = "consultation")
public class PastMedicalHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int historyId;
	
	private String allergeies;
	private String currentMedicine;
	private String medicalHistory;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@ManyToOne
	@JoinColumn(name = "consultation_id")
	private Consultation consultation;
}
