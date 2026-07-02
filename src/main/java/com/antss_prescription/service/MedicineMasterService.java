package com.antss_prescription.service;

import java.util.List;
import java.util.UUID;

import com.antss_prescription.entity.prescription.MedicineMaster;

public interface MedicineMasterService {

    MedicineMaster saveMedicine(MedicineMaster medicine, UUID userId);

    MedicineMaster updateMedicine(Long medicineId, MedicineMaster medicine, UUID userId);

    MedicineMaster getMedicineById(Long medicineId, UUID userId);

    List<MedicineMaster> searchMedicine(String keyword, UUID userId);

    List<MedicineMaster> getAllMedicines(UUID userId);

    void deleteMedicine(Long medicineId, UUID userId);
}
