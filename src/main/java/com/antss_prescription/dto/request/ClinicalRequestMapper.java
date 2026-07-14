package com.antss_prescription.dto.request;

import java.util.ArrayList;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.prescription.CheifComplaints;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.Diagnosis;
import com.antss_prescription.entity.prescription.GeneralExamination;
import com.antss_prescription.entity.prescription.PastMedicalHistory;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Vitals;

public final class ClinicalRequestMapper {
    private ClinicalRequestMapper() {}

    public static PatientRegistration toRegistration(PatientRegistrationRequest request) {
        PatientRegistration registration = new PatientRegistration();
        if (request.getPatient() != null) {
            applyPatient(request.getPatient(), registration);
        }
        if (request.getClinicId() != null) {
            Clinic clinic = new Clinic();
            clinic.setId(request.getClinicId());
            registration.setClinic(clinic);
        }
        if (request.getHospitalId() != null) {
            Hospital hospital = new Hospital();
            hospital.setId(request.getHospitalId());
            registration.setHospital(hospital);
        }
        registration.setStatus(request.getStatus());
        return registration;
    }

    public static PatientRegistration toRegistration(PatientRequest request) {
        PatientRegistration registration = new PatientRegistration();
        applyPatient(request, registration);
        return registration;
    }

    private static void applyPatient(PatientRequest request, PatientRegistration registration) {
        registration.setPatientName(request.getPatientName());
        registration.setMobileNumber(request.getMobileNumber());
        registration.setGender(request.getGender());
        registration.setDateOfBirth(request.getDateOfBirth());
        registration.setAge(request.getAge());
        registration.setAddress(request.getAddress());
        registration.setState(request.getState());
        registration.setCity(request.getCity());
        registration.setPincode(request.getPincode());
    }

    public static Consultation toConsultation(ConsultationRequest request) {
        Consultation consultation = new Consultation();
        Doctor doctor = new Doctor();
        doctor.setId(request.getDoctorId());
        consultation.setDoctor(doctor);
        PatientRegistration registration = new PatientRegistration();
        registration.setRegistrationId(request.getRegistrationId());
        consultation.setPatientRegistration(registration);
        consultation.setConsultationNumber(request.getConsultationNumber());
        consultation.setAdvice(request.getAdvice());
        consultation.setFollowUpDate(request.getFollowUpDate());

        consultation.setCheifComplaints(new ArrayList<>());
        if (request.getComplaints() != null) request.getComplaints().forEach(item -> {
            CheifComplaints value = new CheifComplaints();
            value.setComplaintName(item.getComplaintName());
            value.setFrequency(item.getFrequency());
            value.setSev(item.getSev());
            value.setDuration(item.getDuration());
            consultation.getCheifComplaints().add(value);
        });

        consultation.setGeneralExaminations(new ArrayList<>());
        if (request.getExaminations() != null) request.getExaminations().forEach(item -> {
            GeneralExamination value = new GeneralExamination();
            value.setGeneralExamination(item.getGeneralExamination());
            consultation.getGeneralExaminations().add(value);
        });

        consultation.setDiagnoses(new ArrayList<>());
        if (request.getDiagnoses() != null) request.getDiagnoses().forEach(item -> {
            Diagnosis value = new Diagnosis();
            value.setDiagnosisName(item.getDiagnosisName());
            value.setDiagnosisCode(item.getDiagnosisCode());
            value.setDuration(item.getDuration());
            consultation.getDiagnoses().add(value);
        });

        consultation.setPastMedicalHistories(new ArrayList<>());
        if (request.getMedicalHistories() != null) request.getMedicalHistories().forEach(item -> {
            PastMedicalHistory value = new PastMedicalHistory();
            value.setAllergeies(item.getAllergeies());
            value.setCurrentMedicine(item.getCurrentMedicine());
            value.setMedicalHistory(item.getMedicalHistory());
            consultation.getPastMedicalHistories().add(value);
        });

        if (request.getVitals() != null) {
            Vitals vitals = new Vitals();
            vitals.setHeight(request.getVitals().getHeight());
            vitals.setWeight(request.getVitals().getWeight());
            vitals.setTemprature(request.getVitals().getTemprature());
            vitals.setPulse(request.getVitals().getPulse());
            vitals.setSpo2(request.getVitals().getSpo2());
            vitals.setBp(request.getVitals().getBp());
            vitals.setRespiratoryRate(request.getVitals().getRespiratoryRate());
            consultation.setVitals(vitals);
        }
        return consultation;
    }

    public static Vitals toVitals(UpdateVitalsRequest request) {
        Vitals vitals = new Vitals();
        vitals.setHeight(request.getHeight());
        vitals.setWeight(request.getWeight());
        vitals.setTemprature(request.getTemprature());
        vitals.setPulse(request.getPulse());
        vitals.setSpo2(request.getSpo2());
        vitals.setBp(request.getBp());
        vitals.setRespiratoryRate(request.getRespiratoryRate());
        return vitals;
    }
}
