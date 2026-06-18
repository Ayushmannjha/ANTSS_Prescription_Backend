package com.antss_prescription.controller;

import com.antss_prescription.dto.request.CreatePackageRequest;
import com.antss_prescription.dto.response.ApiResponse;
import com.antss_prescription.dto.response.PackageResponse;
import com.antss_prescription.service.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@Tag(name = "Package APIs", description = "Manage subscription packages")
public class PackageController {

    private final PackageService packageService;


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new package (Admin only)")
    public ResponseEntity<ApiResponse<PackageResponse>> createPackage(@Valid @RequestBody CreatePackageRequest request) {
        PackageResponse response = packageService.createPackage(request);
        return ResponseEntity.ok(ApiResponse.success("Package created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update an existing package (Admin only)")
    public ResponseEntity<ApiResponse<PackageResponse>> updatePackage(@PathVariable Long id, @Valid @RequestBody CreatePackageRequest request) {
        PackageResponse response = packageService.updatePackage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Package updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete a package (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deletePackage(@PathVariable Long id) {
        packageService.deletePackage(id);
        return ResponseEntity.ok(ApiResponse.success("Package deleted successfully"));
    }

    @GetMapping
    @Operation(summary = "List all packages (Public)")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getAllPackages() {
        List<PackageResponse> responses = packageService.getAllPackages();
        return ResponseEntity.ok(ApiResponse.success("Packages fetched successfully", responses));
    }
}
