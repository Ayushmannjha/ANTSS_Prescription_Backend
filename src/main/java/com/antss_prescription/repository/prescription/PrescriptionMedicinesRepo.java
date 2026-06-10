package com.antss_prescription.repository.prescription;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.entity.prescription.PrescriptionMedicines;

@Repository
public interface PrescriptionMedicinesRepo extends JpaRepository<PrescriptionMedicines, Integer>{
List<PrescriptionMedicines> findByPrescription(Prescription prescription);

void deleteByPrescription(Prescription prescription);
}
