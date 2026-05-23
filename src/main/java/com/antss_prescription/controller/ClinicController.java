package com.antss_prescription.controller;

import com.antss_prescription.dto.request.CreateClinicRequest;
import com.antss_prescription.dto.response.ApiResponse;
import com.antss_prescription.dto.response.ClinicResponse;
import com.antss_prescription.entity.User;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.service.ClinicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clinics")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Clinic APIs", description = "Management of Clinic accounts")
public class ClinicController {

    private final ClinicService clinicService;
    private final UserRepository userRepository;

    public ClinicController(ClinicService clinicService, UserRepository userRepository) {
        this.clinicService = clinicService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new clinic (owner only, subject to subscription limit)")
    public ResponseEntity<ApiResponse<ClinicResponse>> createClinic(
            @Valid @RequestBody CreateClinicRequest request) {
        UUID ownerId = getCurrentUserId();
        ClinicResponse response = clinicService.createClinic(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Clinic created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all clinics owned by the logged-in user")
    public ResponseEntity<ApiResponse<List<ClinicResponse>>> listClinics() {
        UUID userId = getCurrentUserId();
        List<ClinicResponse> responses = clinicService.listClinics(userId);
        return ResponseEntity.ok(ApiResponse.success("Clinics fetched successfully", responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a clinic's details by ID")
    public ResponseEntity<ApiResponse<ClinicResponse>> getClinicById(@PathVariable Long id) {
        UUID userId = getCurrentUserId();
        ClinicResponse response = clinicService.getClinicById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Clinic fetched successfully", response));
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
