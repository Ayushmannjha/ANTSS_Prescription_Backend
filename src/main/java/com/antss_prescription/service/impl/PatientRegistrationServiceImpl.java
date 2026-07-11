package com.antss_prescription.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.antss_prescription.dto.response.PatientRegistrationResponse;
import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.exception.ConflictException;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.prescription.ConsultationRepo;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import com.antss_prescription.repository.prescription.PatientRepo;
import com.antss_prescription.security.AccessControlService;
import com.antss_prescription.service.PatientRegistrationService;
import com.antss_prescription.websocket.PatientRegistrationWebSocketHandler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientRegistrationServiceImpl implements PatientRegistrationService {

    private final PatientRegistrationRepo registrationRepo;
    private final PatientRepo patientRepository;
    private final ClinicRepository clinicRepository;
    private final HospitalRepository hospitalRepository;
    private final AccessControlService accessControl;
    private final ConsultationRepo consultationRepository;
    private final PatientRegistrationWebSocketHandler patientRegistrationWebSocketHandler;

    @Override
    @Transactional
    public PatientRegistration saveRegistration(PatientRegistration registration) {
        resolveAndAuthorizeFacility(registration);
        registration.setPatient(resolvePatient(registration.getPatient()));
        registration.setRegistrationNumber(generateRegistrationNumber(registration));
        registration.setCreatedAt(LocalDateTime.now());
        registration.setUpdatedAt(LocalDateTime.now());
        PatientRegistration saved = registrationRepo.save(registration);
        patientRegistrationWebSocketHandler.publishRegistrationCreated(saved);
        return saved;
    }

    private Patient resolvePatient(Patient incomingPatient) {
        if (incomingPatient == null) {
            throw new IllegalArgumentException("Patient details are required");
        }

        String mobileNumber = incomingPatient.getMobileNumber() == null
                ? ""
                : incomingPatient.getMobileNumber().trim();
        if (mobileNumber.isBlank()) {
            throw new IllegalArgumentException("Patient mobile number is required");
        }

        String dateOfBirth = normalize(incomingPatient.getDateOfBirth());
        String gender = normalize(incomingPatient.getGender());

        Patient patient = incomingPatient.getPatientId() > 0
                ? patientRepository.findById(incomingPatient.getPatientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Patient", incomingPatient.getPatientId()))
                : findExistingPatient(mobileNumber, dateOfBirth, gender)
                        .orElse(incomingPatient);

        if (patient.getCreatedAt() == null) {
            patient.setCreatedAt(LocalDateTime.now());
        }

        patient.setPatientName(incomingPatient.getPatientName());
        patient.setMobileNumber(mobileNumber);
        patient.setGender(gender);
        patient.setDateOfBirth(dateOfBirth);
        patient.setAge(incomingPatient.getAge());
        patient.setAddress(incomingPatient.getAddress());
        patient.setState(incomingPatient.getState());
        patient.setCity(incomingPatient.getCity());
        patient.setPincode(incomingPatient.getPincode());
        patient.setUpdatedAt(LocalDateTime.now());
        return patientRepository.save(patient);
    }

    private java.util.Optional<Patient> findExistingPatient(String mobileNumber, String dateOfBirth, String gender) {
        if (dateOfBirth.isBlank() || gender.isBlank()) {
            return java.util.Optional.empty();
        }
        return patientRepository.findFirstByMobileNumberAndDateOfBirthAndGenderIgnoreCase(
                mobileNumber, dateOfBirth, gender);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String generateRegistrationNumber(PatientRegistration registration) {
        String entityName = "";
        if (registration.getClinic() != null) {
            entityName = registration.getClinic().getClinicName();
        } else if (registration.getHospital() != null) {
            entityName = registration.getHospital().getHospitalName();
        }

        String prefix = generatePrefix(entityName);
        String financialYear = getFinancialYear();
        long nextNumber = registrationRepo.nextRegistrationSequenceValue();

        return prefix + financialYear + "/" + nextNumber;
    }

    private String generatePrefix(String entityName) {
        if (entityName == null || entityName.trim().isEmpty()) {
            return "REG";
        }

        String[] words = entityName.trim().toUpperCase().split("\\s+");

        if (words.length == 1) {
            return words[0].length() >= 3 ? words[0].substring(0, 3) : words[0];
        }

        String basePrefix = words[0].length() >= 3 ? words[0].substring(0, 3) : words[0];
        return resolvePrefix(basePrefix, words, entityName);
    }

    private String resolvePrefix(String basePrefix, String[] words, String fullName) {
        List<PatientRegistration> existing = registrationRepo
                .findByRegistrationNumberStartingWith(basePrefix + getFinancialYear());

        if (existing.isEmpty() || registrationsBelongToSameEntity(existing, fullName)) {
            return basePrefix;
        }

        if (words[0].length() >= 4) {
            String extended = words[0].substring(0, 4);
            List<PatientRegistration> extendedCheck = registrationRepo
                    .findByRegistrationNumberStartingWith(extended + getFinancialYear());

            if (extendedCheck.isEmpty() || registrationsBelongToSameEntity(extendedCheck, fullName)) {
                return extended;
            }
        }

        if (words.length > 1) {
            return basePrefix + words[1].charAt(0);
        }

        return basePrefix;
    }

    private boolean registrationsBelongToSameEntity(List<PatientRegistration> registrations, String fullName) {
        return registrations.stream().allMatch(reg -> {
            String existingName = "";
            if (reg.getClinic() != null) {
                existingName = reg.getClinic().getClinicName();
            } else if (reg.getHospital() != null) {
                existingName = reg.getHospital().getHospitalName();
            }
            return existingName.equalsIgnoreCase(fullName);
        });
    }

    private String getFinancialYear() {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        int startYear;
        int endYear;
        if (month >= 4) {
            startYear = year;
            endYear = year + 1;
        } else {
            startYear = year - 1;
            endYear = year;
        }

        return String.valueOf(startYear).substring(2) + String.valueOf(endYear).substring(2);
    }

    @Override
    public PatientRegistrationResponse getRegistrationById(Integer registrationId) {
        PatientRegistration registration = registrationRepo.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found with id : " + registrationId));
        accessControl.requireRegistrationAccess(registration);
        return toResponse(registration);
    }

    @Override
    public List<PatientRegistrationResponse> getAllRegistrations() {
        var scope = accessControl.currentClinicalScope();
        List<PatientRegistration> registrations = scope.admin()
                ? registrationRepo.findAll()
                : registrationRepo.findAccessible(scope.hospitalIds(), scope.clinicIds());
        return registrations.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public PatientRegistrationResponse updateRegistration(Integer registrationId, PatientRegistration registration) {
        PatientRegistration existing = registrationRepo.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found with id : " + registrationId));

        accessControl.requireRegistrationAccess(existing);
        resolveAndAuthorizeFacility(registration);
        existing.setPatient(resolvePatient(registration.getPatient()));
        existing.setClinic(registration.getClinic());
        existing.setHospital(registration.getHospital());
        existing.setStatus(registration.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        return toResponse(registrationRepo.save(existing));
    }

    @Override
    public void deleteRegistration(Integer registrationId) {
        PatientRegistration registration = registrationRepo.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("Registration not found with id : " + registrationId));

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
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PatientRegistrationResponse> getAllRegistrationsByHospital(Hospital hospital) {
        Hospital managedHospital = hospitalRepository.findById(hospital.getId())
                .orElseThrow(() -> new RuntimeException("Hospital not found: " + hospital.getId()));
        accessControl.requireFacilityAccess(managedHospital);
        return registrationRepo.findByHospital(managedHospital)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PatientRegistrationResponse toResponse(PatientRegistration registration) {
        PatientRegistrationResponse response = new PatientRegistrationResponse();
        response.setRegistrationId(registration.getRegistrationId());
        response.setRegistrationNumber(registration.getRegistrationNumber());
        response.setPatient(registration.getPatient());
        response.setStatus(registration.getStatus());
        response.setCreatedAt(registration.getCreatedAt());
        response.setUpdatedAt(registration.getUpdatedAt());

        if (registration.getClinic() != null) {
            response.setClinicId(registration.getClinic().getId());
            response.setClinicName(registration.getClinic().getClinicName());
        }

        if (registration.getHospital() != null) {
            response.setHospitalId(registration.getHospital().getId());
            response.setHospitalName(registration.getHospital().getHospitalName());
        }

        return response;
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
