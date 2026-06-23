package com.antss_prescription.service.impl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.antss_prescription.dto.response.PatientRegistrationResponse;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.exception.DuplicateRegistrationException;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import com.antss_prescription.repository.prescription.PatientRepo;
import com.antss_prescription.repository.prescription.ConsultationRepo;
import com.antss_prescription.exception.ConflictException;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.service.PatientRegistrationService;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class PatientRegistrationServiceImpl
        implements PatientRegistrationService {

    private final PatientRegistrationRepo registrationRepo;
    private final PatientRepo patientRepository;
    private final ClinicRepository clinicRepository;
    private final HospitalRepository hospitalRepository;
    private final AccessControlService accessControl;
    private final ConsultationRepo consultationRepository;

    @Override
    @Transactional
    public PatientRegistration saveRegistration(PatientRegistration registration) {

        resolveAndAuthorizeFacility(registration);

        // Handle Patient — create if not exists, reuse if exists
        if (registration.getPatient() != null) {
            Patient patient = registration.getPatient();

            if (patient.getPatientId() != 0) {
                // Patient ID provided — fetch existing patient
                Patient existingPatient = patientRepository.findById(patient.getPatientId())
                        .orElseThrow(() -> new RuntimeException(
                                "Patient not found with id : " + patient.getPatientId()));

                // Check if this patient is already registered at the same clinic/hospital
                checkDuplicateRegistration(existingPatient, registration);

                registration.setPatient(existingPatient);
            } else {
                // No patientId — create new patient directly
                patient.setCreatedAt(LocalDateTime.now());
                patient.setUpdatedAt(LocalDateTime.now());
                Patient savedPatient = patientRepository.save(patient);
                registration.setPatient(savedPatient);
            }
        }

        // Generate registration number
        String registrationNumber = generateRegistrationNumber(registration);
        registration.setRegistrationNumber(registrationNumber);

        registration.setCreatedAt(LocalDateTime.now());
        registration.setUpdatedAt(LocalDateTime.now());

        return registrationRepo.save(registration);
    }
    private void checkDuplicateRegistration(Patient patient, PatientRegistration registration) {

        if (registration.getClinic() != null) {
            boolean alreadyRegistered = registrationRepo
                    .existsByPatientAndClinic(patient, registration.getClinic());
            if (alreadyRegistered) {
                PatientRegistration existing = registrationRepo
                        .findByPatientAndClinic(patient, registration.getClinic())
                        .orElse(null);
                String existingRegNo = existing != null ? existing.getRegistrationNumber() : "N/A";
                throw new DuplicateRegistrationException(
                        "Patient is already registered at this clinic. " +
                        "Existing registration number : " + existingRegNo);
            }
        }

        if (registration.getHospital() != null) {
            boolean alreadyRegistered = registrationRepo
                    .existsByPatientAndHospital(patient, registration.getHospital());
            if (alreadyRegistered) {
                PatientRegistration existing = registrationRepo
                        .findByPatientAndHospital(patient, registration.getHospital())
                        .orElse(null);
                String existingRegNo = existing != null ? existing.getRegistrationNumber() : "N/A";
                throw new DuplicateRegistrationException(
                        "Patient is already registered at this hospital. " +
                        "Existing registration number : " + existingRegNo);
            }
        }
    }
    private String generateRegistrationNumber(PatientRegistration registration) {
    	//System.out.println("===Patient Registration===");
    	//System.out.println(registration.getClinic().getClinicName());
        // Step 1 — Get entity name (clinic or hospital)
        String entityName = "";
        if (registration.getClinic() != null) {
            entityName = registration.getClinic().getClinicName();
        } else if (registration.getHospital() != null) {
            entityName = registration.getHospital().getHospitalName();
        }

        // Step 2 — Generate prefix from entity name
        String prefix = generatePrefix(entityName);

        // Step 3 — Generate financial year suffix e.g. 2627 for 2026-27
        String financialYear = getFinancialYear();

        // Step 4 — Get next sequence number for this prefix + financial year
        String prefixWithYear = prefix + financialYear;
        long nextNumber = registrationRepo.nextRegistrationSequenceValue();

        return prefixWithYear + "/" + nextNumber;
    }

    private String generatePrefix(String entityName) {
        if (entityName == null || entityName.trim().isEmpty()) {
            return "REG";
        }

        // Split name into words and take first 3 letters of each word
        String[] words = entityName.trim().toUpperCase().split("\\s+");

        if (words.length == 1) {
            // Single word — take first 3 letters e.g. "PARVATI" -> "PAR"
            return words[0].length() >= 3 ? words[0].substring(0, 3) : words[0];
        }

        // Multiple words — check for duplicate prefixes
        // Build prefix from first letters of each word first e.g. "PARVATI HOSPITAL" -> "PH"
        // But we need at least 3 chars, so take first 3 from first word
        String basePrefix = words[0].length() >= 3 ? words[0].substring(0, 3) : words[0];

        // Check if this prefix is already used by another clinic/hospital
        // If conflict, extend prefix by adding next char from first word or first char of second word
        String finalPrefix = resolvePrefix(basePrefix, words, entityName);

        return finalPrefix;
    }

    private String resolvePrefix(String basePrefix, String[] words, String fullName) {
        // Check if prefix already exists in DB for a different entity
        List<PatientRegistration> existing = registrationRepo
                .findByRegistrationNumberStartingWith(basePrefix + getFinancialYear());

        if (existing.isEmpty()) {
            return basePrefix; // No conflict
        }

        // Check if all existing records belong to the same entity name
        boolean sameEntity = existing.stream().allMatch(reg -> {
            String existingName = "";
            if (reg.getClinic() != null) existingName = reg.getClinic().getClinicName();
            else if (reg.getHospital() != null) existingName = reg.getHospital().getHospitalName();
            return existingName.equalsIgnoreCase(fullName);
        });

        if (sameEntity) {
            return basePrefix; // Same entity, reuse prefix
        }

        // Conflict — extend prefix
        // Try adding 4th character from first word
        if (words[0].length() >= 4) {
            String extended = words[0].substring(0, 4);
            List<PatientRegistration> extendedCheck = registrationRepo
                    .findByRegistrationNumberStartingWith(extended + getFinancialYear());

            if (extendedCheck.isEmpty()) {
                return extended;
            }

            // Still conflict — check same entity again
            boolean sameEntityExtended = extendedCheck.stream().allMatch(reg -> {
                String existingName = "";
                if (reg.getClinic() != null) existingName = reg.getClinic().getClinicName();
                else if (reg.getHospital() != null) existingName = reg.getHospital().getHospitalName();
                return existingName.equalsIgnoreCase(fullName);
            });

            if (sameEntityExtended) {
                return extended;
            }
        }

        // Last resort — use first 3 chars of first word + first char of second word
        if (words.length > 1) {
            String combined = basePrefix + words[1].charAt(0);
            return combined;
        }

        return basePrefix;
    }

    private String getFinancialYear() {
        // Financial year: April to March
        // 2026 April - 2027 March = "2627"
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        int startYear, endYear;
        if (month >= 4) {
            // April onwards — current FY
            startYear = year;
            endYear = year + 1;
        } else {
            // Jan to March — previous FY
            startYear = year - 1;
            endYear = year;
        }

        // Take last 2 digits of each year
        String start = String.valueOf(startYear).substring(2);
        String end = String.valueOf(endYear).substring(2);

        return start + end;
    }

    @Override
    public PatientRegistrationResponse getRegistrationById(Integer registrationId) {

        PatientRegistration reg = registrationRepo.findById(registrationId)
                .orElseThrow(() -> new RuntimeException(
                        "Registration not found with id : " + registrationId));

        accessControl.requireRegistrationAccess(reg);
        PatientRegistrationResponse response = new PatientRegistrationResponse();
        response.setRegistrationId(reg.getRegistrationId());
        response.setRegistrationNumber(reg.getRegistrationNumber());
        response.setPatient(reg.getPatient());

        if (reg.getClinic() != null) {
            response.setClinicId(reg.getClinic().getId());
            response.setClinicName(reg.getClinic().getClinicName());
        }

        if (reg.getHospital() != null) {
            response.setHospitalId(reg.getHospital().getId());
            response.setHospitalName(reg.getHospital().getHospitalName());
        }

        return response;
    }
    @Override
    public List<PatientRegistrationResponse> getAllRegistrations() {
        var scope = accessControl.currentClinicalScope();
        List<PatientRegistration> registrations = scope.admin()
                ? registrationRepo.findAll()
                : registrationRepo.findAccessible(scope.hospitalIds(), scope.clinicIds());
        return registrations
                .stream()
                .map(reg -> {
                    PatientRegistrationResponse response = new PatientRegistrationResponse();
                    response.setRegistrationId(reg.getRegistrationId());
                    response.setRegistrationNumber(reg.getRegistrationNumber());
                    response.setPatient(reg.getPatient());

                    if (reg.getClinic() != null) {
                        response.setClinicId(reg.getClinic().getId());
                        response.setClinicName(reg.getClinic().getClinicName());
                    }

                    if (reg.getHospital() != null) {
                        response.setHospitalId(reg.getHospital().getId());
                        response.setHospitalName(reg.getHospital().getHospitalName());
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public PatientRegistrationResponse updateRegistration(
            Integer registrationId,
            PatientRegistration registration) {

        PatientRegistration existing = registrationRepo.findById(registrationId)
                .orElseThrow(() -> new RuntimeException(
                        "Registration not found with id : " + registrationId));

        accessControl.requireRegistrationAccess(existing);
        resolveAndAuthorizeFacility(registration);
        existing.setPatient(registration.getPatient());
        existing.setClinic(registration.getClinic());
        existing.setHospital(registration.getHospital());
        existing.setStatus(registration.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        PatientRegistration saved = registrationRepo.save(existing);

        PatientRegistrationResponse response = new PatientRegistrationResponse();
        response.setRegistrationId(saved.getRegistrationId());
        response.setRegistrationNumber(saved.getRegistrationNumber());
        response.setPatient(saved.getPatient());

        if (saved.getClinic() != null) {
            response.setClinicId(saved.getClinic().getId());
            response.setClinicName(saved.getClinic().getClinicName());
        }

        if (saved.getHospital() != null) {
            response.setHospitalId(saved.getHospital().getId());
            response.setHospitalName(saved.getHospital().getHospitalName());
        }

        return response;
    }

    @Override
    public void deleteRegistration(Integer registrationId) {

        PatientRegistration registration =
                registrationRepo.findById(registrationId)
                        .orElseThrow(() -> new RuntimeException(
                                "Registration not found with id : "
                                        + registrationId));

        accessControl.requireRegistrationAccess(registration);
        if (consultationRepository.existsByPatientRegistration(registration)) {
            throw new ConflictException("Registration cannot be deleted while consultations exist");
        }
        registrationRepo.delete(registration);
    }

	
    @Override
    public List<PatientRegistrationResponse> getAllRegistrationsByClinic(Clinic clinic) {
        Clinic managedClinic = clinicRepository.findById(clinic.getId())
                .orElseThrow(() -> new RuntimeException("Clinic not found: " + clinic.getId()));
        accessControl.requireFacilityAccess(managedClinic);
        return registrationRepo.findByClinic(managedClinic)
                .stream()
                .map(reg -> {
                    PatientRegistrationResponse response = new PatientRegistrationResponse();
                    response.setRegistrationId(reg.getRegistrationId());
                    response.setRegistrationNumber(reg.getRegistrationNumber());
                    response.setPatient(reg.getPatient());
                    response.setClinicId(reg.getClinic().getId());
                    response.setClinicName(reg.getClinic().getClinicName());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PatientRegistrationResponse> getAllRegistrationsByHospital(Hospital hospital) {
        Hospital managedHospital = hospitalRepository.findById(hospital.getId())
                .orElseThrow(() -> new RuntimeException("Hospital not found: " + hospital.getId()));
        accessControl.requireFacilityAccess(managedHospital);
        return registrationRepo.findByHospital(managedHospital)
                .stream()
                .map(reg -> {
                    PatientRegistrationResponse response = new PatientRegistrationResponse();
                    response.setRegistrationId(reg.getRegistrationId());
                    response.setRegistrationNumber(reg.getRegistrationNumber());
                    response.setPatient(reg.getPatient());
                    response.setHospitalId(reg.getHospital().getId());
                    response.setHospitalName(reg.getHospital().getHospitalName());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void resolveAndAuthorizeFacility(PatientRegistration registration) {
        boolean hasClinic = registration.getClinic() != null && registration.getClinic().getId() != null;
        boolean hasHospital = registration.getHospital() != null && registration.getHospital().getId() != null;
        if (hasClinic == hasHospital) {
            throw new IllegalArgumentException("Exactly one clinic or hospital is required");
        }
        if (hasClinic) {
            Clinic clinic = clinicRepository.findById(registration.getClinic().getId())
                    .orElseThrow(() -> new RuntimeException("Clinic not found: " + registration.getClinic().getId()));
            accessControl.requireFacilityAccess(clinic);
            registration.setClinic(clinic);
            registration.setHospital(null);
        } else {
            Hospital hospital = hospitalRepository.findById(registration.getHospital().getId())
                    .orElseThrow(() -> new RuntimeException("Hospital not found: " + registration.getHospital().getId()));
            accessControl.requireFacilityAccess(hospital);
            registration.setHospital(hospital);
            registration.setClinic(null);
        }
    }
}
