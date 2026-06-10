package com.antss_prescription.entity.prescription;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name="cheif_complaints")
@Setter
@Getter
@ToString
public class CheifComplaints {

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
}
