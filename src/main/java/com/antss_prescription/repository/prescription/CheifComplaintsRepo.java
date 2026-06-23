package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.CheifComplaints;

@Repository
public interface CheifComplaintsRepo extends JpaRepository<CheifComplaints, Integer> {

}
