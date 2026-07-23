package com.antss_prescription.service;

import com.antss_prescription.dto.request.ConsultationBillDiscountRequest;
import com.antss_prescription.dto.response.ConsultationBillResponse;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.enums.DiscountPolicy;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

public interface ConsultationBillService {
    ConsultationBillResponse generateBill(Consultation consultation, DiscountPolicy discountPolicy, BigDecimal discountValue);
    ConsultationBillResponse getBillById(Long billId);
    ConsultationBillResponse getBillByConsultationId(Integer consultationId);
    ConsultationBillResponse updateDiscount(Long billId, ConsultationBillDiscountRequest request);
    void writeBillPdf(OutputStream outputStream, Long billId) throws IOException;
}
