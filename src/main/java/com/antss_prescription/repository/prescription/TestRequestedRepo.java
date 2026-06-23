package com.antss_prescription.repository.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.antss_prescription.entity.prescription.TestRequested;

@Repository
public interface TestRequestedRepo extends JpaRepository<TestRequested, Integer> {

}
