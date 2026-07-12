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
import lombok.ToString;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name="cheif_complaints")
@Setter
@Getter
@ToString(exclude = "consultation")
public class CheifComplaints implements ClinicalAttribution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int cheifComplaintId;
	private String complaintName;
	private String frequency;
	private String sev;
	private String duration;
	private LocalDateTime complaintDate;
	private LocalDateTime CreatedAt;
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
