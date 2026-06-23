package com.antss_prescription.controller;

import java.util.List;

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
import com.antss_prescription.dto.request.MedicineMasterRequest;
import com.antss_prescription.service.MedicineMasterService;
import com.antss_prescription.security.AccessControlService;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineMasterController {

    private final MedicineMasterService medicineService;
    private final AccessControlService accessControl;

    @PostMapping
    public ResponseEntity<MedicineMaster> saveMedicine(
            @Valid @RequestBody MedicineMasterRequest request) {

        MedicineMaster medicine = new MedicineMaster();
        medicine.setMedicineName(request.getMedicineName());
        medicine.setGenericName(request.getGenericName());
        medicine.setStrength(request.getStrength());
        medicine.setDosageForm(request.getDosageForm());
        medicine.setManufacturer(request.getManufacturer());
        medicine.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());

        return ResponseEntity.ok(
                medicineService.saveMedicine(medicine, accessControl.currentUser().getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicineMaster> getMedicineById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                medicineService.getMedicineById(id, accessControl.currentUser().getId()));
    }

    @GetMapping
    public ResponseEntity<List<MedicineMaster>> getAllMedicines(
            ) {

        return ResponseEntity.ok(
                medicineService.getAllMedicines(accessControl.currentUser().getId()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MedicineMaster>> searchMedicine(
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                medicineService.searchMedicine(keyword, accessControl.currentUser().getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMedicine(
            @PathVariable Long id) {

        medicineService.deleteMedicine(id, accessControl.currentUser().getId());

        return ResponseEntity.ok("Medicine deleted successfully");
    }
}
