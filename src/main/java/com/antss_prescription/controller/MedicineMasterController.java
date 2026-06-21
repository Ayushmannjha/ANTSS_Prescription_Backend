package com.antss_prescription.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.antss_prescription.entity.prescription.MedicineMaster;
import com.antss_prescription.service.MedicineMasterService;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineMasterController {

    private final MedicineMasterService medicineService;

    @PostMapping
    public ResponseEntity<MedicineMaster> saveMedicine(
            @Valid @RequestBody MedicineMaster medicine,
            @RequestParam UUID userId) {

        return ResponseEntity.ok(
                medicineService.saveMedicine(medicine, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicineMaster> getMedicineById(
            @PathVariable Long id,
            @RequestParam UUID userId) {

        return ResponseEntity.ok(
                medicineService.getMedicineById(id, userId));
    }

    @GetMapping
    public ResponseEntity<List<MedicineMaster>> getAllMedicines(
            @RequestParam UUID userId) {

        return ResponseEntity.ok(
                medicineService.getAllMedicines(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MedicineMaster>> searchMedicine(
            @RequestParam String keyword,
            @RequestParam UUID userId) {

        return ResponseEntity.ok(
                medicineService.searchMedicine(keyword, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMedicine(
            @PathVariable Long id,
            @RequestParam UUID userId) {

        medicineService.deleteMedicine(id, userId);

        return ResponseEntity.ok("Medicine deleted successfully");
    }
}
