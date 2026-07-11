package com.antss_prescription.entity.prescription;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String fileName;
    private String url;
    private String documentType;
    private String cloudinaryPublicId;
    private String cloudinaryResourceType;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    private PatientRegistration patientRegistration;

    @JsonIgnore
    @ManyToOne
	@JoinColumn(name = "prescription_id")
	private Prescription prescription;
}
