package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
@Entity
@Table(name = "diagnosis")
public class Diagnosis {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int  diagnosisId;
	private String diagnosisName;
	private String diagnosisCode;
	private String duration;
	private LocalDateTime diagnosisDate;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
