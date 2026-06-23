package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.GeneralExamination;

@Repository
public interface GeneralExaminationRepo extends JpaRepository<GeneralExamination, Integer> {

}
