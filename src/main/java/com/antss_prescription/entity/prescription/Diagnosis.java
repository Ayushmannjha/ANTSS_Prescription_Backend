package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;
import com.antss_prescription.entity.Doctor;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.ToString;

@Setter
@Getter
@ToString(exclude = "consultation")
@Entity
@Table(name = "diagnosis")
public class Diagnosis implements ClinicalAttribution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int  diagnosisId;
	private String diagnosisName;
	private String diagnosisCode;
	private String duration;
	private LocalDateTime diagnosisDate;
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
