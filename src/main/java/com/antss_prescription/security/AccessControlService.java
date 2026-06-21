package com.antss_prescription.security;

import com.antss_prescription.entity.Clinic;
import com.antss_prescription.entity.Doctor;
import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.Rmo;
import com.antss_prescription.entity.User;
import com.antss_prescription.entity.prescription.Consultation;
import com.antss_prescription.entity.prescription.Patient;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.entity.prescription.Prescription;
import com.antss_prescription.enums.EntityStatus;
import com.antss_prescription.enums.Role;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.exception.ForbiddenException;
import com.antss_prescription.exception.UnauthorizedException;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.DoctorRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.RmoRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final RmoRepository rmoRepository;
    private final PatientRegistrationRepo registrationRepository;

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Authentication is required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
    }

    public User resolveSubscriptionOwner(User user) {
        if (isAdmin(user)) {
            return user;
        }
        if (user.getUserType() == UserType.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ForbiddenException("Doctor profile is not linked to this account"));
            return ownerOf(doctor.getHospital(), doctor.getClinic());
        }
        if (user.getUserType() == UserType.RMO) {
            Rmo rmo = rmoRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ForbiddenException("RMO profile is not linked to this account"));
            return ownerOf(rmo.getHospital(), rmo.getClinic());
        }
        if (user.getUserType() == UserType.HOSPITAL) {
            Hospital hospital = hospitalRepository.findByUserId(user.getId()).stream().findFirst().orElse(null);
            return hospital != null && hospital.getOwner() != null ? hospital.getOwner() : user;
        }
        if (user.getUserType() == UserType.CLINIC) {
            Clinic clinic = clinicRepository.findByUserId(user.getId()).stream().findFirst().orElse(null);
            return clinic != null && clinic.getOwner() != null ? clinic.getOwner() : user;
        }
        throw new ForbiddenException("User type is not linked to a subscription owner");
    }

    public boolean canAccess(Hospital hospital, User user) {
        if (hospital == null) return false;
        if (isAdmin(user) || sameUser(hospital.getUser(), user) || sameUser(hospital.getOwner(), user)) return true;
        if (user.getUserType() == UserType.DOCTOR) {
            return doctorRepository.findByUserId(user.getId())
                    .map(d -> d.getHospital() != null && d.getHospital().getId().equals(hospital.getId()))
                    .orElse(false);
        }
        if (user.getUserType() == UserType.RMO) {
            return rmoRepository.findByUserId(user.getId())
                    .map(r -> r.getHospital() != null && r.getHospital().getId().equals(hospital.getId()))
                    .orElse(false);
        }
        return false;
    }

    public boolean canAccess(Clinic clinic, User user) {
        if (clinic == null) return false;
        if (isAdmin(user) || sameUser(clinic.getUser(), user) || sameUser(clinic.getOwner(), user)) return true;
        if (user.getUserType() == UserType.DOCTOR) {
            return doctorRepository.findByUserId(user.getId())
                    .map(d -> d.getClinic() != null && d.getClinic().getId().equals(clinic.getId()))
                    .orElse(false);
        }
        if (user.getUserType() == UserType.RMO) {
            return rmoRepository.findByUserId(user.getId())
                    .map(r -> r.getClinic() != null && r.getClinic().getId().equals(clinic.getId()))
                    .orElse(false);
        }
        return false;
    }

    public void requireFacilityAccess(Hospital hospital) {
        if (!canAccess(hospital, currentUser())) throw forbidden();
    }

    public void requireFacilityAccess(Clinic clinic) {
        if (!canAccess(clinic, currentUser())) throw forbidden();
    }

    public boolean canAccess(PatientRegistration registration, User user) {
        return registration != null
                && (canAccess(registration.getHospital(), user) || canAccess(registration.getClinic(), user));
    }

    public void requireRegistrationAccess(PatientRegistration registration) {
        if (!canAccess(registration, currentUser())) throw forbidden();
    }

    public boolean canAccess(Patient patient, User user) {
        if (patient == null) return false;
        if (isAdmin(user)) return true;
        return registrationRepository.findByPatient(patient).stream()
                .anyMatch(registration -> canAccess(registration, user));
    }

    public void requirePatientAccess(Patient patient) {
        if (!canAccess(patient, currentUser())) throw forbidden();
    }

    public boolean canAccess(Consultation consultation, User user) {
        return consultation != null && canAccess(consultation.getPatientRegistration(), user);
    }

    public void requireConsultationAccess(Consultation consultation) {
        if (!canAccess(consultation, currentUser())) throw forbidden();
    }

    public boolean canAccess(Prescription prescription, User user) {
        return prescription != null && canAccess(prescription.getConsultation(), user);
    }

    public void requirePrescriptionAccess(Prescription prescription) {
        if (!canAccess(prescription, currentUser())) throw forbidden();
    }

    public Doctor requireCurrentDoctorFor(PatientRegistration registration) {
        User user = currentUser();
        if (user.getUserType() != UserType.DOCTOR) {
            throw new ForbiddenException("Only doctors can create prescriptions");
        }
        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ForbiddenException("Doctor profile is not linked to this account"));
        if (doctor.getStatus() != EntityStatus.ACTIVE || !canAccess(registration, user)) {
            throw forbidden();
        }
        return doctor;
    }

    public void requireDoctorAccess(Doctor doctor) {
        User user = currentUser();
        if (doctor == null || !(isAdmin(user) || sameUser(doctor.getUser(), user)
                || canAccess(doctor.getHospital(), user) || canAccess(doctor.getClinic(), user))) {
            throw forbidden();
        }
    }

    public void requireDoctorForRegistration(Doctor doctor, PatientRegistration registration) {
        requireDoctorAccess(doctor);
        boolean sameHospital = doctor.getHospital() != null && registration.getHospital() != null
                && doctor.getHospital().getId().equals(registration.getHospital().getId());
        boolean sameClinic = doctor.getClinic() != null && registration.getClinic() != null
                && doctor.getClinic().getId().equals(registration.getClinic().getId());
        if (!sameHospital && !sameClinic) {
            throw new ForbiddenException("Doctor and patient registration must belong to the same facility");
        }
    }

    public void requireAdmin() {
        if (!isAdmin(currentUser())) throw new ForbiddenException("Administrator access is required");
    }

    private User ownerOf(Hospital hospital, Clinic clinic) {
        if (hospital != null) return hospital.getOwner() != null ? hospital.getOwner() : hospital.getUser();
        if (clinic != null) return clinic.getOwner() != null ? clinic.getOwner() : clinic.getUser();
        throw new ForbiddenException("Account is not associated with a hospital or clinic");
    }


    private boolean sameUser(User left, User right) {
        return left != null && right != null && left.getId().equals(right.getId());
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ROLE_ADMIN;
    }

    private ForbiddenException forbidden() {
        return new ForbiddenException("You do not have access to this clinical resource");
    }
}
