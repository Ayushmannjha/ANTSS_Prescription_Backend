package com.antss_prescription.controller;

import com.antss_prescription.dto.request.CreateHospitalRequest;
import com.antss_prescription.dto.response.ApiResponse;
import com.antss_prescription.dto.response.HospitalResponse;
import com.antss_prescription.entity.User;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.service.HospitalService;
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
@RequestMapping("/api/hospitals")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Hospital APIs", description = "Management of Hospital accounts")
public class HospitalController {

    private final HospitalService hospitalService;
    private final UserRepository userRepository;

    public HospitalController(HospitalService hospitalService, UserRepository userRepository) {
        this.hospitalService = hospitalService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new hospital (owner only, subject to subscription limit)")
    public ResponseEntity<ApiResponse<HospitalResponse>> createHospital(
            @Valid @RequestBody CreateHospitalRequest request) {
        UUID ownerId = getCurrentUserId();
        HospitalResponse response = hospitalService.createHospital(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hospital created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List all hospitals owned by the logged-in user")
    public ResponseEntity<ApiResponse<List<HospitalResponse>>> listHospitals() {
        UUID userId = getCurrentUserId();
        List<HospitalResponse> responses = hospitalService.listHospitals(userId);
        return ResponseEntity.ok(ApiResponse.success("Hospitals fetched successfully", responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a hospital's details by ID")
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospitalById(@PathVariable Long id) {
        UUID userId = getCurrentUserId();
        HospitalResponse response = hospitalService.getHospitalById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Hospital fetched successfully", response));
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
