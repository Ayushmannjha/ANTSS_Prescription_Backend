package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
	private LocalDateTime createdAt;
}
