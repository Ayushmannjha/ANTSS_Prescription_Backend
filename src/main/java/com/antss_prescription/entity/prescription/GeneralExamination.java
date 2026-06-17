package com.antss_prescription.entity.prescription;

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
@Table(name="general_examination")
@Setter
@Getter
@ToString(exclude = "consultation")
public class GeneralExamination {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int generalExaminationId;
	private String generalExamination;

	@ManyToOne
	@JoinColumn(name = "consultation_id")
	private Consultation consultation;
}
