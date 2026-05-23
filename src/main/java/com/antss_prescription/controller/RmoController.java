package com.antss_prescription.controller;

import com.antss_prescription.dto.request.CreateRmoRequest;
import com.antss_prescription.dto.response.ApiResponse;
import com.antss_prescription.dto.response.RmoResponse;
import com.antss_prescription.entity.User;
import com.antss_prescription.exception.ResourceNotFoundException;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.service.RmoService;
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
@RequestMapping("/api/rmos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "RMO APIs", description = "Management of RMOs and staff members")
public class RmoController {

    private final RmoService rmoService;
    private final UserRepository userRepository;

    public RmoController(RmoService rmoService, UserRepository userRepository) {
        this.rmoService = rmoService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Add a new RMO/Staff member")
    public ResponseEntity<ApiResponse<RmoResponse>> addRmo(@Valid @RequestBody CreateRmoRequest request) {
        UUID userId = getCurrentUserId();
        RmoResponse response = rmoService.addRmo(request, userId);
        return ResponseEntity.ok(ApiResponse.success("RMO added successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an RMO/Staff member's details")
    public ResponseEntity<ApiResponse<RmoResponse>> updateRmo(@PathVariable UUID id, @Valid @RequestBody CreateRmoRequest request) {
        UUID userId = getCurrentUserId();
        RmoResponse response = rmoService.updateRmo(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("RMO updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Mark an RMO/Staff member as inactive")
    public ResponseEntity<ApiResponse<Void>> deleteRmo(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        rmoService.deleteRmo(id, userId);
        return ResponseEntity.ok(ApiResponse.success("RMO deleted successfully"));
    }

    @GetMapping
    @Operation(summary = "List all RMOs and staff belonging to the logged-in Hospital or Clinic")
    public ResponseEntity<ApiResponse<List<RmoResponse>>> listRmos() {
        UUID userId = getCurrentUserId();
        List<RmoResponse> responses = rmoService.listRmos(userId);
        return ResponseEntity.ok(ApiResponse.success("RMOs fetched successfully", responses));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get RMO/Staff details by ID")
    public ResponseEntity<ApiResponse<RmoResponse>> getRmoById(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        RmoResponse response = rmoService.getRmoById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("RMO fetched successfully", response));
    }

    private UUID getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }
}
