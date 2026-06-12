package com.antss_prescription.controller;

import com.antss_prescription.dto.request.CreateDoctorRequest;
import com.antss_prescription.dto.request.UpdateDoctorRequest;
import com.antss_prescription.dto.response.ApiResponse;
import com.antss_prescription.dto.response.DoctorResponse;
import com.antss_prescription.entity.User;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Doctor APIs", description = "Management of doctors for Hospitals and Clinics")
public class DoctorController {

    private final DoctorService doctorService;
    private final UserRepository userRepository;

    public DoctorController(DoctorService doctorService, UserRepository userRepository) {
        this.doctorService = doctorService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Add a new doctor")
    public ResponseEntity<ApiResponse<DoctorResponse>> addDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        UUID userId = getCurrentUserId();
        DoctorResponse response = doctorService.addDoctor(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Doctor added successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing doctor's details")
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(@PathVariable UUID id, @Valid @RequestBody UpdateDoctorRequest request) {
        UUID userId = getCurrentUserId();
        DoctorResponse response = doctorService.updateDoctor(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (deactivate) a doctor")
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        doctorService.deleteDoctor(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Doctor deleted successfully"));
    }

    @GetMapping
    @Operation(summary = "List all doctors belonging to the logged-in Hospital or Clinic")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> listDoctors() {
        UUID userId = getCurrentUserId();
        List<DoctorResponse> responses = doctorService.listDoctors(userId);
        return ResponseEntity.ok(ApiResponse.success("Doctors fetched successfully", responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a doctor's details by ID")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        DoctorResponse response = doctorService.getDoctorById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Doctor fetched successfully", response));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current doctor's profile details")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorProfile() {
        UUID userId = getCurrentUserId();
        DoctorResponse response = doctorService.getDoctorByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Doctor profile fetched successfully", response));
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
