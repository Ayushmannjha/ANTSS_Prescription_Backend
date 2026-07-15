package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.Consultation;
import java.util.List;
import java.util.UUID;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.enums.ConsultationStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ConsultationRepo extends JpaRepository<Consultation, Integer> {
    List<Consultation> findByDoctorIdOrderByCreatedAtDesc(UUID doctorId);
    List<Consultation> findByDoctorIdAndStatusOrderByRequestedAtDesc(UUID doctorId, ConsultationStatus status);
    List<Consultation> findByDoctorIdAndStatusInOrderByRequestedAtDesc(UUID doctorId, List<ConsultationStatus> statuses);
    @Query("""
            SELECT c FROM Consultation c
            WHERE c.doctor.id = :doctorId
              AND (c.status IN :statuses OR c.status IS NULL)
            ORDER BY COALESCE(c.requestedAt, c.createdAt) DESC
            """)
    List<Consultation> findDoctorDashboardConsultations(@Param("doctorId") UUID doctorId,
            @Param("statuses") List<ConsultationStatus> statuses);
    boolean existsByPatientRegistration(PatientRegistration registration);

    @Query("""
            SELECT c FROM Consultation c JOIN c.patientRegistration r
            WHERE r.hospital.id IN :hospitalIds OR r.clinic.id IN :clinicIds
            """)
    List<Consultation> findAccessible(@Param("hospitalIds") List<Long> hospitalIds,
            @Param("clinicIds") List<Long> clinicIds);
}
