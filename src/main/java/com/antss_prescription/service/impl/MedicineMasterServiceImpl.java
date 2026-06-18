package com.antss_prescription.service.impl;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.antss_prescription.entity.User;
import com.antss_prescription.entity.prescription.MedicineMaster;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.prescription.MedicineMasterRepository;
import com.antss_prescription.service.MedicineMasterService;

@Service
@RequiredArgsConstructor
public class MedicineMasterServiceImpl implements MedicineMasterService {

    private final MedicineMasterRepository medicineRepository;
    private final UserRepository userRepository;


    @Override
    public MedicineMaster saveMedicine(MedicineMaster medicine, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        medicine.setUser(user);

        return medicineRepository.save(medicine);
    }

    @Override
    public MedicineMaster getMedicineById(Long medicineId, UUID userId) {

        return medicineRepository
                .findByMedicineIdAndUserId(medicineId, userId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
    }

    @Override
    public List<MedicineMaster> searchMedicine(String keyword, UUID userId) {

        return medicineRepository
                .findByUserIdAndMedicineNameContainingIgnoreCase(
                        userId,
                        keyword
                );
    }

    @Override
    public List<MedicineMaster> getAllMedicines(UUID userId) {

        return medicineRepository.findByUserId(userId);
    }

    @Override
    public void deleteMedicine(Long medicineId, UUID userId) {

        MedicineMaster medicine = medicineRepository
                .findByMedicineIdAndUserId(medicineId, userId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));

        medicineRepository.delete(medicine);
    }
}