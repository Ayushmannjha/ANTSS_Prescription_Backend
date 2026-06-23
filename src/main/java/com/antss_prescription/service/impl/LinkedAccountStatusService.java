package com.antss_prescription.service.impl;

import com.antss_prescription.entity.*;
import com.antss_prescription.enums.EntityStatus;
import com.antss_prescription.enums.LoginStatus;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkedAccountStatusService {

    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final RmoRepository rmoRepository;
    private final UserRepository userRepository;
    private final LoginCredentialRepository credentialRepository;
    private final LoginSessionRepository sessionRepository;

    @Transactional
    public void expireOwnerAndLinkedAccounts(User owner) {
        Set<UUID> processedUsers = new HashSet<>();
        expireUser(owner, processedUsers);

        for (Hospital hospital : hospitalRepository.findByOwnerId(owner.getId())) {
            hospital.setStatus(EntityStatus.INACTIVE);
            hospitalRepository.save(hospital);
            expireUser(hospital.getUser(), processedUsers);
            doctorRepository.findByHospital(hospital).forEach(doctor -> {
                doctor.setStatus(EntityStatus.INACTIVE);
                doctorRepository.save(doctor);
                expireUser(doctor.getUser(), processedUsers);
            });
            rmoRepository.findByHospital(hospital).forEach(rmo -> {
                rmo.setStatus(EntityStatus.INACTIVE);
                rmoRepository.save(rmo);
                expireUser(rmo.getUser(), processedUsers);
            });
        }

        for (Clinic clinic : clinicRepository.findByOwnerId(owner.getId())) {
            clinic.setStatus(EntityStatus.INACTIVE);
            clinicRepository.save(clinic);
            expireUser(clinic.getUser(), processedUsers);
            doctorRepository.findByClinic(clinic).forEach(doctor -> {
                doctor.setStatus(EntityStatus.INACTIVE);
                doctorRepository.save(doctor);
                expireUser(doctor.getUser(), processedUsers);
            });
            rmoRepository.findByClinic(clinic).forEach(rmo -> {
                rmo.setStatus(EntityStatus.INACTIVE);
                rmoRepository.save(rmo);
                expireUser(rmo.getUser(), processedUsers);
            });
        }
    }

    @Transactional
    public void reactivateOwnerAndSubscriptionExpiredAccounts(User owner) {
        Set<UUID> processedUsers = new HashSet<>();
        reactivateUserIfExpired(owner, processedUsers);

        for (Hospital hospital : hospitalRepository.findByOwnerId(owner.getId())) {
            if (hospital.getUser().getStatus() == RegistrationStatus.EXPIRED) {
                hospital.setStatus(EntityStatus.ACTIVE);
                hospitalRepository.save(hospital);
            }
            reactivateUserIfExpired(hospital.getUser(), processedUsers);
            doctorRepository.findByHospital(hospital).forEach(doctor -> {
                if (doctor.getUser() != null && doctor.getUser().getStatus() == RegistrationStatus.EXPIRED) {
                    doctor.setStatus(EntityStatus.ACTIVE);
                    doctorRepository.save(doctor);
                }
                reactivateUserIfExpired(doctor.getUser(), processedUsers);
            });
            rmoRepository.findByHospital(hospital).forEach(rmo -> {
                if (rmo.getUser() != null && rmo.getUser().getStatus() == RegistrationStatus.EXPIRED) {
                    rmo.setStatus(EntityStatus.ACTIVE);
                    rmoRepository.save(rmo);
                }
                reactivateUserIfExpired(rmo.getUser(), processedUsers);
            });
        }

        for (Clinic clinic : clinicRepository.findByOwnerId(owner.getId())) {
            if (clinic.getUser().getStatus() == RegistrationStatus.EXPIRED) {
                clinic.setStatus(EntityStatus.ACTIVE);
                clinicRepository.save(clinic);
            }
            reactivateUserIfExpired(clinic.getUser(), processedUsers);
            doctorRepository.findByClinic(clinic).forEach(doctor -> {
                if (doctor.getUser() != null && doctor.getUser().getStatus() == RegistrationStatus.EXPIRED) {
                    doctor.setStatus(EntityStatus.ACTIVE);
                    doctorRepository.save(doctor);
                }
                reactivateUserIfExpired(doctor.getUser(), processedUsers);
            });
            rmoRepository.findByClinic(clinic).forEach(rmo -> {
                if (rmo.getUser() != null && rmo.getUser().getStatus() == RegistrationStatus.EXPIRED) {
                    rmo.setStatus(EntityStatus.ACTIVE);
                    rmoRepository.save(rmo);
                }
                reactivateUserIfExpired(rmo.getUser(), processedUsers);
            });
        }
    }

    private void expireUser(User user, Set<UUID> processedUsers) {
        if (user == null || !processedUsers.add(user.getId())) return;
        user.setStatus(RegistrationStatus.EXPIRED);
        userRepository.save(user);
        credentialRepository.findByUserId(user.getId()).ifPresent(credential -> {
            credential.setLoginStatus(LoginStatus.BLOCKED);
            credentialRepository.save(credential);
        });
        sessionRepository.expireAllSessionsForUser(user);
    }

    private void reactivateUserIfExpired(User user, Set<UUID> processedUsers) {
        if (user == null || !processedUsers.add(user.getId())
                || user.getStatus() != RegistrationStatus.EXPIRED) return;
        user.setStatus(RegistrationStatus.APPROVED);
        userRepository.save(user);
        credentialRepository.findByUserId(user.getId()).ifPresent(credential -> {
            credential.setLoginStatus(LoginStatus.ACTIVE);
            credentialRepository.save(credential);
        });
    }
}
