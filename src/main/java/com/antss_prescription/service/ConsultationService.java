package com.antss_prescription.service;

import java.util.List;
import java.util.UUID;

import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.Vitals;

public interface ConsultationService {

    ConsultationResponse saveConsultation(Consultation consultation);
    ConsultationResponse getConsultationById(Integer consultationId);
    List<ConsultationResponse> getAllConsultations();
    List<ConsultationResponse> getConsultationsByDoctor(UUID doctorId);
    ConsultationResponse updateConsultation(Integer consultationId, Consultation consultation);
    ConsultationResponse updateVitals(Integer consultationId, Vitals vitals);
    void deleteConsultation(Integer consultationId);
}
