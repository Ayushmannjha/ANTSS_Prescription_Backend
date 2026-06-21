package com.antss_prescription.security;

import com.antss_prescription.entity.Hospital;
import com.antss_prescription.entity.User;
import com.antss_prescription.entity.prescription.PatientRegistration;
import com.antss_prescription.enums.Role;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.repository.ClinicRepository;
import com.antss_prescription.repository.DoctorRepository;
import com.antss_prescription.repository.HospitalRepository;
import com.antss_prescription.repository.RmoRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.prescription.PatientRegistrationRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private HospitalRepository hospitalRepository;
    @Mock private ClinicRepository clinicRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private RmoRepository rmoRepository;
    @Mock private PatientRegistrationRepo registrationRepository;

    private AccessControlService accessControl;

    @BeforeEach
    void setUp() {
        accessControl = new AccessControlService(userRepository, hospitalRepository, clinicRepository,
                doctorRepository, rmoRepository, registrationRepository);
    }

    @Test
    void childHospitalAccountResolvesOwnersSubscription() {
        User owner = user(UserType.HOSPITAL);
        User child = user(UserType.HOSPITAL);
        Hospital hospital = new Hospital();
        hospital.setId(10L);
        hospital.setUser(child);
        hospital.setOwner(owner);
        when(hospitalRepository.findByUserId(child.getId())).thenReturn(List.of(hospital));

        assertEquals(owner.getId(), accessControl.resolveSubscriptionOwner(child).getId());
    }

    @Test
    void hospitalOwnerCanAccessOwnedChildButUnrelatedUserCannot() {
        User owner = user(UserType.HOSPITAL);
        User child = user(UserType.HOSPITAL);
        User unrelated = user(UserType.HOSPITAL);
        Hospital hospital = new Hospital();
        hospital.setId(11L);
        hospital.setUser(child);
        hospital.setOwner(owner);

        assertTrue(accessControl.canAccess(hospital, owner));
        assertTrue(accessControl.canAccess(hospital, child));
        assertFalse(accessControl.canAccess(hospital, unrelated));

        PatientRegistration registration = new PatientRegistration();
        registration.setHospital(hospital);
        assertTrue(accessControl.canAccess(registration, owner));
        assertFalse(accessControl.canAccess(registration, unrelated));
    }

    private User user(UserType type) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUserType(type);
        user.setRole(Role.ROLE_USER);
        return user;
    }
}
