package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.Investigations;

@Repository
public interface InvestigationsRepo extends JpaRepository<Investigations, Integer> {

}
