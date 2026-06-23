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
import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Vitals;

public final class ClinicalRequestMapper {
    private ClinicalRequestMapper() {}

    public static Patient toPatient(PatientRequest request) {
        Patient patient = new Patient();
        patient.setPatientName(request.getPatientName());
        patient.setMobileNumber(request.getMobileNumber());
        patient.setGender(request.getGender());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setAge(request.getAge());
        patient.setAddress(request.getAddress());
        patient.setState(request.getState());
        patient.setCity(request.getCity());
        patient.setPincode(request.getPincode());
        return patient;
    }

    public static PatientRegistration toRegistration(PatientRegistrationRequest request) {
        PatientRegistration registration = new PatientRegistration();
        Patient patient = request.getPatient() == null ? new Patient() : toPatient(request.getPatient());
        if (request.getPatientId() != null) patient.setPatientId(request.getPatientId());
        registration.setPatient(patient);
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
}
