package com.antss_prescription.controller;

import com.antss_prescription.dto.request.ConsultationBillDiscountRequest;
import com.antss_prescription.dto.response.ConsultationBillResponse;
import com.antss_prescription.service.ConsultationBillService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultation-bills")
public class ConsultationBillController {

    private final ConsultationBillService consultationBillService;

    @GetMapping("/{billId}")
    public ConsultationBillResponse getBillById(@PathVariable Long billId) {
        return consultationBillService.getBillById(billId);
    }

    @GetMapping("/consultations/{consultationId}")
    public ConsultationBillResponse getBillByConsultationId(@PathVariable Integer consultationId) {
        return consultationBillService.getBillByConsultationId(consultationId);
    }

    @PatchMapping("/{billId}/discount")
    public ConsultationBillResponse updateDiscount(
            @PathVariable Long billId,
            @Valid @RequestBody ConsultationBillDiscountRequest request) {
        return consultationBillService.updateDiscount(billId, request);
    }

    @GetMapping("/{billId}/pdf")
    public void downloadBillPdf(
            @PathVariable Long billId,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=consultation-bill-" + billId + ".pdf");
        consultationBillService.writeBillPdf(response.getOutputStream(), billId);
    }
}
