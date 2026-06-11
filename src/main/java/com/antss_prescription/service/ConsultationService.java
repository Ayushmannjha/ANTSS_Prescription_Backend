package com.antss_prescription.service;

import java.util.List;
import java.util.UUID;

import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.entity.prescription.Consultation;

public interface ConsultationService {

    ConsultationResponse saveConsultation(Consultation consultation);
    ConsultationResponse getConsultationById(Integer consultationId);
    List<ConsultationResponse> getAllConsultations();
    List<ConsultationResponse> getConsultationsByDoctor(UUID doctorId);
    ConsultationResponse updateConsultation(Integer consultationId, Consultation consultation);
    void deleteConsultation(Integer consultationId);
}