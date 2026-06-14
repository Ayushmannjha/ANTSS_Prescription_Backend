package com.antss_prescription.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.stereotype.Service;

import com.antss_prescription.entity.prescription.Investigations;
import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.repository.prescription.InvestigationsRepo;
import com.antss_prescription.repository.prescription.PrescriptionRepo;
import com.antss_prescription.service.InvestigationsService;

@Service
public class InvestigationsServiceImpl implements InvestigationsService {

    private final AuthenticationProvider authenticationProvider;

    // =========================
    // Autowired
    // =========================

    @Autowired
    private InvestigationsRepo investigationsRepo;
    @Autowired
    private PrescriptionRepo prescriptionRepo;

    InvestigationsServiceImpl(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    // =========================
    // Save
    // =========================

    @Override
    public Investigations save(Investigations investigations) {

        try {

            System.out.println(
                    "==== SAVING INVESTIGATION STARTED ====");

            if (investigations.getId() == 0) {

                investigations.setCreateAt(
                        LocalDateTime.now());
            }

            investigations.setUpdatedAt(
                    LocalDateTime.now());

            Investigations saved =
                    investigationsRepo.save(
                            investigations);

            System.out.println(
                    "==== INVESTIGATION SAVED : "
                            + saved.getId()
                            + " ====");

            return saved;

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Failed To Save Investigation : "
                            + ex.getMessage());
        }
    }

    // =========================
    // Get By Registration Number
    // =========================

    @Override
    public List<Investigations> getByRegistrationNumber(
            String registrationNumber) {

        try {

            System.out.println(
                    "==== FETCHING INVESTIGATIONS BY REGISTRATION NUMBER : "
                            + registrationNumber
                            + " ====");

            List<Investigations> list =

                    investigationsRepo
                            .findByPatientRegistrationRegistrationNumber(
                                    registrationNumber);

            System.out.println(
                    "==== INVESTIGATIONS FOUND : "
                            + list.size()
                            + " ====");

            return list;

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Failed To Fetch Investigations : "
                            + ex.getMessage());
        }
    }

    // =========================
    // Get By Document ID
    // =========================

    @Override
    public List<Investigations> getByDocumentId(
            Integer documentId) {

        try {

            System.out.println(
                    "==== FETCHING INVESTIGATIONS BY DOCUMENT ID : "
                            + documentId
                            + " ====");

            List<Investigations> list =

                    investigationsRepo
                            .findByDocumentId(
                                    documentId);

            System.out.println(
                    "==== INVESTIGATIONS FOUND : "
                            + list.size()
                            + " ====");

            return list;

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Failed To Fetch Investigations By Document : "
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
                    "==== DELETING INVESTIGATION : "
                            + id
                            + " ====");

            investigationsRepo.deleteById(id);

            System.out.println(
                    "==== INVESTIGATION DELETED ====");

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Failed To Delete Investigation : "
                            + ex.getMessage());
        }
    }

    @Override
    public List<Investigations> getByPrescription(Integer prescriptionId) {

        Prescription prescription =

                prescriptionRepo
                        .findById(prescriptionId)
                        .orElseThrow(() -> new RuntimeException(
                                "Prescription Not Found : "
                                        + prescriptionId));

        return investigationsRepo
                .findByPrescription(prescription);
    }
}