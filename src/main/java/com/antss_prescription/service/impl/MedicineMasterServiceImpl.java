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
import com.antss_prescription.security.AccessControlService;

@Service
@RequiredArgsConstructor
public class MedicineMasterServiceImpl implements MedicineMasterService {

    private final MedicineMasterRepository medicineRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControl;


    @Override
    public MedicineMaster saveMedicine(MedicineMaster medicine, UUID userId) {

        userId = requireCurrentUserId(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        medicine.setUser(user);

        return medicineRepository.save(medicine);
    }

    @Override
    public MedicineMaster getMedicineById(Long medicineId, UUID userId) {

        userId = requireCurrentUserId(userId);

        return medicineRepository
                .findByMedicineIdAndUserId(medicineId, userId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));
    }

    @Override
    public List<MedicineMaster> searchMedicine(String keyword, UUID userId) {

        userId = requireCurrentUserId(userId);

        return medicineRepository
                .findByUserIdAndMedicineNameContainingIgnoreCase(
                        userId,
                        keyword
                );
    }

    @Override
    public List<MedicineMaster> getAllMedicines(UUID userId) {

        userId = requireCurrentUserId(userId);

        return medicineRepository.findByUserId(userId);
    }

    @Override
    public void deleteMedicine(Long medicineId, UUID userId) {

        userId = requireCurrentUserId(userId);

        MedicineMaster medicine = medicineRepository
                .findByMedicineIdAndUserId(medicineId, userId)
                .orElseThrow(() -> new RuntimeException("Medicine not found"));

        medicineRepository.delete(medicine);
    }

    private UUID requireCurrentUserId(UUID requestedUserId) {
        UUID currentUserId = accessControl.currentUser().getId();
        if (!currentUserId.equals(requestedUserId)) {
            throw new com.antss_prescription.exception.ForbiddenException(
                    "Medicine records can only be accessed by their owner");
        }
        return currentUserId;
    }
}
