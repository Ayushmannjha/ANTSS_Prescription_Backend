package com.antss_prescription.entity.prescription;

import com.antss_prescription.entity.Doctor;
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
@Table(name="general_examination")
@Setter
@Getter
@ToString(exclude = "consultation")
public class GeneralExamination implements ClinicalAttribution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int generalExaminationId;
	private String generalExamination;
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
