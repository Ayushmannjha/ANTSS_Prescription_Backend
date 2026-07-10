package com.antss_prescription.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.antss_prescription.dto.response.ApiResponse;
import com.antss_prescription.dto.response.DetailedPrescriptionResponse;
import com.antss_prescription.entity.prescription.PrintHeaders;
import com.antss_prescription.service.PrescriptionPrintService;
import com.antss_prescription.service.PrescriptionService;
import com.antss_prescription.service.PrintHeadersService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/print-headers")
@RequiredArgsConstructor
public class PrescriptionPrintController {

	private final PrintHeadersService printHeadersService;
	private final PrescriptionService prescriptionService;
	private final PrescriptionPrintService prescriptionPrintService;

	@Value("${app.base-url:http://localhost:2030}")
	private String appBaseUrl;

	@PostMapping
	public ResponseEntity<ApiResponse<PrintHeaders>> uploadHeader(
			@RequestParam("entityId") long entityId,
			@RequestParam("entityType") String entityType,
			@RequestParam("image") MultipartFile image) {

		PrintHeaders headers = printHeadersService.uploadHeader(entityId, entityType, image);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Print header uploaded successfully", headers));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<PrintHeaders>>> getHeaders(
			@RequestParam(value = "entityId", required = false) Long entityId,
			@RequestParam(value = "entityType", required = false) String entityType) {

		List<PrintHeaders> headers = printHeadersService.getHeaders(entityId, entityType);
		return ResponseEntity.ok(ApiResponse.success("Print headers fetched successfully", headers));
	}

	@GetMapping("/{headerId}/prescriptions/{prescriptionId}/pdf")
	public void downloadPrescriptionPdf(
			@PathVariable long headerId,
			@PathVariable int prescriptionId,
			HttpServletResponse response) throws IOException {

		DetailedPrescriptionResponse prescription =
				prescriptionService.getDetailedPrescriptionForPrintById(prescriptionId);
		String downloadUrl = buildPrescriptionPdfUrl(headerId, prescriptionId);

		writePdfResponse(response, headerId, prescriptionId, prescription, downloadUrl);
	}

	@GetMapping("/prescriptions/{prescriptionId}/pdf")
	public void downloadPrescriptionPdfWithDefaultHeader(
			@PathVariable int prescriptionId,
			HttpServletResponse response) throws IOException {

		DetailedPrescriptionResponse prescription =
				prescriptionService.getDetailedPrescriptionForPrintById(prescriptionId);
		String downloadUrl = buildDefaultPrescriptionPdfUrl(prescriptionId);

		writePdfResponse(response, null, prescriptionId, prescription, downloadUrl);
	}

	private String buildPrescriptionPdfUrl(long headerId, int prescriptionId) {
		return trimTrailingSlash(appBaseUrl)
				+ "/api/print-headers/"
				+ headerId
				+ "/prescriptions/"
				+ prescriptionId
				+ "/pdf";
	}

	private String buildDefaultPrescriptionPdfUrl(int prescriptionId) {
		return trimTrailingSlash(appBaseUrl)
				+ "/api/print-headers/prescriptions/"
				+ prescriptionId
				+ "/pdf";
	}

	private void writePdfResponse(
			HttpServletResponse response,
			Long headerId,
			int prescriptionId,
			DetailedPrescriptionResponse prescription,
			String downloadUrl) throws IOException {

		response.setContentType("application/pdf");
		response.setHeader(
				"Content-Disposition",
				"attachment; filename=prescription-" + prescriptionId + ".pdf");

		prescriptionPrintService.writePrescriptionPdf(
				response.getOutputStream(),
				headerId,
				prescription,
				downloadUrl);
	}

	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return "http://localhost:2030";
		}
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
