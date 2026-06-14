package com.antss_prescription.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.antss_prescription.entity.User;
import com.antss_prescription.entity.prescription.MedicineMaster;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.prescription.MedicineMasterRepository;
import com.antss_prescription.service.MedicineMasterService;

@Service
public class MedicineMasterServiceImpl implements MedicineMasterService {

    private final MedicineMasterRepository medicineRepository;
    private final UserRepository userRepository;

    public MedicineMasterServiceImpl(
            MedicineMasterRepository medicineRepository,
            UserRepository userRepository) {

        this.medicineRepository = medicineRepository;
        this.userRepository = userRepository;
    }

    @Override
    public MedicineMaster saveMedicine(MedicineMaster medicine, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        medicine.setUser(user);

        return medicineRepository.save(medicine);
    }

    @Override
    public MedicineMaster getMedicineById(Long medicineId, Long userId) {

        return medicineRepository
                .findByMedicineIdAndUserUserId(medicineId, userId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
    }

    @Override
    public List<MedicineMaster> searchMedicine(String keyword, UUID userId) {

        return medicineRepository
                .findByUserUserIdAndMedicineNameContainingIgnoreCase(
                        userId,
                        keyword
                );
    }

    @Override
    public List<MedicineMaster> getAllMedicines(Long userId) {

        return medicineRepository.findByUserUserId(userId);
    }

    @Override
    public void deleteMedicine(Long medicineId, Long userId) {

        MedicineMaster medicine = medicineRepository
                .findByMedicineIdAndUserUserId(medicineId, userId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));

        medicineRepository.delete(medicine);
    }
}