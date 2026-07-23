package com.antss_prescription.repository.prescription;

import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.ConsultationBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsultationBillRepository extends JpaRepository<ConsultationBill, Long> {
    boolean existsByBillNumber(String billNumber);
    Optional<ConsultationBill> findByBillNumber(String billNumber);
    Optional<ConsultationBill> findByConsultation(Consultation consultation);
    Optional<ConsultationBill> findByConsultationConsultationId(Integer consultationId);
}
