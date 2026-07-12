package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;
import com.antss_prescription.entity.Doctor;

import jakarta.annotation.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
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
public class PastMedicalHistory implements ClinicalAttribution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int historyId;
	
	private String allergeies;
	private String currentMedicine;
	private String medicalHistory;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	@ManyToOne
	@JoinColumn(name = "doctor_id", nullable = true)
	private Doctor doctorReference;
	@Column(name = "entity_type", nullable = true, length = 20)
	private String entityType;
	@Column(name = "entity_id", nullable = true)
	private Long entityId;

	@ManyToOne
	@JoinColumn(name = "consultation_id")
	private Consultation consultation;
}
