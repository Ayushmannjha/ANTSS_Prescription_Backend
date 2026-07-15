package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import com.antss_prescription.entity.Rmo;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@Entity
@Setter
@Getter
@ToString
public class Vitals {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int vitalId;
	private int height;//height is in centimeter
	private double weight;//it can be 44.2kg
	private double temprature;//degree foreign height
	private double pulse;//unit will be bpm (blood per minute)
	private double spo2;//unit will be percentage;
	private String bp; // it will be bp_systolic/bp_diastolic
	private double respiratoryRate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "registration_id", nullable = true)
	private PatientRegistration patientRegistration;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recorded_by_rmo_id", nullable = true)
	private Rmo recordedBy;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
