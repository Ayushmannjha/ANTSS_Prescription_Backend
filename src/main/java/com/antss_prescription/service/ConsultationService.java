package com.antss_prescription.service;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import com.antss_prescription.dto.response.ConsultationResponse;
import com.antss_prescription.dto.response.DoctorOptionResponseDto;
import com.antss_prescription.dto.response.VitalsResponseDto;
import com.antss_prescription.dto.request.CreateConsultRequestDto;
import com.antss_prescription.dto.request.VitalsRequestDto;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.Vitals;
import com.antss_prescription.enums.DiscountPolicy;

public interface ConsultationService {

    ConsultationResponse saveConsultation(Consultation consultation, DiscountPolicy discountPolicy, BigDecimal discountValue);
    ConsultationResponse getConsultationById(Integer consultationId);
    List<ConsultationResponse> getAllConsultations();
    List<ConsultationResponse> getConsultationsByDoctor(UUID doctorId);
    List<ConsultationResponse> getMyConsultationRequests();
    List<DoctorOptionResponseDto> getAvailableDoctorsForRegistration(Integer registrationId);
    VitalsResponseDto saveVitals(Integer registrationId, VitalsRequestDto request);
    ConsultationResponse createConsultRequest(CreateConsultRequestDto request);
    ConsultationResponse startConsultation(Integer consultationId);
    ConsultationResponse completeConsultation(Integer consultationId);
    ConsultationResponse updateConsultation(Integer consultationId, Consultation consultation);
    ConsultationResponse updateVitals(Integer consultationId, Vitals vitals);
    void deleteConsultation(Integer consultationId);
}
