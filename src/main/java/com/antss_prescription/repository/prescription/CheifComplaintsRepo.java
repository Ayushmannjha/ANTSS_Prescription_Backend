package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.CheifComplaints;
import java.util.List;
import java.util.UUID;

@Repository
public interface CheifComplaintsRepo extends JpaRepository<CheifComplaints, Integer> {
    List<CheifComplaints> findByDoctorReference_IdAndEntityTypeIgnoreCaseAndEntityId(UUID doctorId, String entityType, Long entityId);
}
