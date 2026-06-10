package com.antss_prescription.repository.prescription;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.CheifComplaints;

@Repository
public interface CheifComplaintsRepo extends JpaRepository<CheifComplaints, Integer> {

    List<CheifComplaints> findByComplaintNameContainingIgnoreCase(String complaintName);

    List<CheifComplaints> findByFrequency(String frequency);

}