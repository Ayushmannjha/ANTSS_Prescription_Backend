package com.antss_prescription.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.entity.prescription.TestRequested;
import com.antss_prescription.repository.prescription.PrescriptionRepo;
import com.antss_prescription.repository.prescription.TestRequestedRepo;
import com.antss_prescription.service.TestRequestedService;

@Service
public class TestRequestedServiceImpl implements TestRequestedService {

    // =========================
    // Autowired
    // =========================

    @Autowired
    private TestRequestedRepo testRequestedRepo;

    @Autowired
    private PrescriptionRepo prescriptionRepo;

    // =========================
    // Save
    // =========================

    @Override
    public TestRequested save(TestRequested testRequested) {

        try {

            System.out.println(
                    "==== SAVING TEST REQUESTED STARTED ====");

            if (testRequested.getId() == 0) {

                testRequested.setCreateAt(
                        LocalDateTime.now());
            }

            testRequested.setUpdatedAt(
                    LocalDateTime.now());

            TestRequested saved =
                    testRequestedRepo.save(
                            testRequested);

            System.out.println(
                    "==== TEST REQUESTED SAVED : "
                            + saved.getId()
                            + " ====");

            return saved;

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Failed To Save Test Requested : "
                            + ex.getMessage());
        }
    }

    // =========================
    // Get By Registration Number
    // =========================

    @Override
    public List<TestRequested> getByRegistrationNumber(
            String registrationNumber) {

        try {

            System.out.println(
                    "==== FETCHING TEST REQUESTED BY REGISTRATION NUMBER : "
                            + registrationNumber
                            + " ====");

            List<TestRequested> list =

                    testRequestedRepo
                            .findByPatientRegistrationRegistrationNumber(
                                    registrationNumber);

            System.out.println(
                    "==== TEST REQUESTED FOUND : "
                            + list.size()
                            + " ====");

            return list;

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Failed To Fetch Test Requested : "
                            + ex.getMessage());
        }
    }

    // =========================
    // Get By Document ID
    // =========================

    @Override
    public List<TestRequested> getByDocumentId(
            Integer documentId) {

        try {

            System.out.println(
                    "==== FETCHING TEST REQUESTED BY DOCUMENT ID : "
                            + documentId
                            + " ====");

            List<TestRequested> list =

                    testRequestedRepo
                            .findByDocumentId(
                                    documentId);

            System.out.println(
                    "==== TEST REQUESTED FOUND : "
                            + list.size()
                            + " ====");

            return list;

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Failed To Fetch Test Requested By Document : "
                            + ex.getMessage());
        }
    }

    // =========================
    // Delete By ID
    // =========================

    @Override
    public void deleteById(Integer id) {

        try {

            System.out.println(
                    "==== DELETING TEST REQUESTED : "
                            + id
                            + " ====");

            testRequestedRepo.deleteById(id);

            System.out.println(
                    "==== TEST REQUESTED DELETED ====");

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Failed To Delete Test Requested : "
                            + ex.getMessage());
        }
    }

    // =========================
    // Get By Prescription
    // =========================

    @Override
    public List<TestRequested> getByPrescription(
            Integer prescriptionId) {

        try {

            System.out.println(
                    "==== FETCHING TEST REQUESTED BY PRESCRIPTION ID : "
                            + prescriptionId
                            + " ====");

            Prescription prescription =

                    prescriptionRepo
                            .findById(prescriptionId)
                            .orElseThrow(() -> new RuntimeException(
                                    "Prescription Not Found : "
                                            + prescriptionId));

            List<TestRequested> list =

                    testRequestedRepo
                            .findByPrescription(
                                    prescription);

            System.out.println(
                    "==== TEST REQUESTED FOUND : "
                            + list.size()
                            + " ====");

            return list;

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Failed To Fetch Test Requested By Prescription : "
                            + ex.getMessage());
        }
    }
}