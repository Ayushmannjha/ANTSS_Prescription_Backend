package com.antss_prescription;

import com.antss_prescription.dto.request.*;
import com.antss_prescription.dto.response.*;
import com.antss_prescription.entity.*;
import com.antss_prescription.enums.*;
import com.antss_prescription.repository.*;
import com.antss_prescription.service.*;
import com.antss_prescription.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest
@Transactional
public class DynamicFlowIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private ClinicService clinicService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private ClinicRepository clinicRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;

    @Autowired
    private PackageRepository packageRepository;

    @MockitoBean
    private EmailService emailService;

    @Test
    public void testDynamicRegistrationApprovalLimitCheckAndDoctorCreation() {
        // 1. Get a seeded package
        SubscriptionPackage goldPackage = packageRepository.findAll().stream()
                .filter(p -> p.getPackageName().contains("Gold"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Gold Package not found"));

        // 2. Register Owner for a Hospital with limit of 2 hospitals
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setFullName("Hospital Owner");
        registerReq.setEmail("owner@hospital.com");
        registerReq.setMobileNumber("1234567890");
        registerReq.setUserType(UserType.HOSPITAL);
        registerReq.setEntityName("Main Hospital");
        registerReq.setAddressLine1("123 Main St");
        registerReq.setCity("City");
        registerReq.setState("State");
        registerReq.setPincode("123456");
        registerReq.setPackageId(goldPackage.getId());
        registerReq.setPassword("Password123");
        registerReq.setConfirmPassword("Password123");
        registerReq.setAllowedHospitals(2); // Set custom limit to 2
        registerReq.setAllowedClinics(0);
        registerReq.setAllowedDoctors(3); // Set custom limit to 3

        authService.register(registerReq);

        User owner = userRepository.findByEmail("owner@hospital.com").orElseThrow();
        assertEquals(RegistrationStatus.PENDING, owner.getStatus());

        // 3. Admin Approves Owner
        UserResponse approvedOwner = adminService.approveUser(owner.getId());
        assertEquals(RegistrationStatus.APPROVED, approvedOwner.getStatus());
        
        // Verify email sent with secure password
        verify(emailService, times(1)).sendApprovalEmail(eq("owner@hospital.com"), eq("Hospital Owner"), any(String.class));

        // Get the active subscription
        List<UserSubscription> subscriptions = userSubscriptionRepository.findByUserId(owner.getId());
        assertFalse(subscriptions.isEmpty());
        UserSubscription subscription = subscriptions.get(0);
        assertEquals(2, subscription.getAllowedHospitals());
        assertEquals(3, subscription.getAllowedDoctors());

        // The initial hospital is automatically created during registration
        List<Hospital> hospitals = hospitalRepository.findByUserIdOrOwnerId(owner.getId(), owner.getId());
        assertEquals(1, hospitals.size());
        assertEquals("Main Hospital", hospitals.get(0).getHospitalName());
        assertEquals(owner.getId(), hospitals.get(0).getOwner().getId());
        assertEquals(owner.getId(), hospitals.get(0).getUser().getId());

        // 4. Owner adds a second hospital (dynamically)
        CreateHospitalRequest createHospitalReq = new CreateHospitalRequest();
        createHospitalReq.setHospitalName("Branch Hospital");
        createHospitalReq.setEmail("branch@hospital.com");
        createHospitalReq.setMobileNumber("0987654321");
        createHospitalReq.setAddressLine1("456 Branch Ave");
        createHospitalReq.setCity("City");
        createHospitalReq.setState("State");
        createHospitalReq.setPincode("654321");

        HospitalResponse branchHospital = hospitalService.createHospital(createHospitalReq, owner.getId());
        assertNotNull(branchHospital);
        assertEquals("Branch Hospital", branchHospital.getHospitalName());

        // Verify credentials email is sent to the branch hospital
        verify(emailService, times(1)).sendCredentialsEmail(
                eq("branch@hospital.com"),
                eq("Branch Hospital"),
                eq("branch@hospital.com"),
                any(String.class),
                eq("Hospital"),
                eq(subscription.getEndDate())
        );

        // Fetching list should now return both hospitals
        List<HospitalResponse> ownerHospitals = hospitalService.listHospitals(owner.getId());
        assertEquals(2, ownerHospitals.size());

        // Try adding a third hospital (should exceed limit)
        CreateHospitalRequest exceedHospitalReq = new CreateHospitalRequest();
        exceedHospitalReq.setHospitalName("Third Hospital");
        exceedHospitalReq.setEmail("third@hospital.com");
        exceedHospitalReq.setMobileNumber("1111222233");
        exceedHospitalReq.setAddressLine1("789 Excess Rd");
        exceedHospitalReq.setCity("City");
        exceedHospitalReq.setState("State");
        exceedHospitalReq.setPincode("111222");

        assertThrows(BusinessException.class, () -> {
            hospitalService.createHospital(exceedHospitalReq, owner.getId());
        });

        // 5. Add Doctor to the branch hospital
        CreateDoctorRequest createDoctorReq = new CreateDoctorRequest();
        createDoctorReq.setDoctorName("Dr. John Doe");
        createDoctorReq.setEmail("john.doe@hospital.com");
        createDoctorReq.setMobileNumber("5556667777");
        createDoctorReq.setSpecialization("Cardiology");
        createDoctorReq.setQualification("MD");
        createDoctorReq.setExperienceYears(10);
        createDoctorReq.setHospitalId(branchHospital.getId());

        // Get branch hospital user id to add doctor (simulate branch login)
        Hospital dbBranch = hospitalRepository.findById(branchHospital.getId()).orElseThrow();
        UUID branchUserId = dbBranch.getUser().getId();

        DoctorResponse doctorRes = doctorService.addDoctor(createDoctorReq, branchUserId);
        assertNotNull(doctorRes);
        assertEquals("Dr. John Doe", doctorRes.getDoctorName());
        assertEquals(branchHospital.getId(), doctorRes.getHospitalId());

        // Verify the Doctor entity has an associated user account created
        Doctor dbDoctor = doctorRepository.findById(doctorRes.getId()).orElseThrow();
        assertNotNull(dbDoctor.getUser());
        assertEquals("john.doe@hospital.com", dbDoctor.getUser().getEmail());
        assertEquals(Role.ROLE_USER, dbDoctor.getUser().getRole());
        assertEquals(UserType.DOCTOR, dbDoctor.getUser().getUserType());

        // Verify credentials email is sent to the doctor
        verify(emailService, times(1)).sendCredentialsEmail(
                eq("john.doe@hospital.com"),
                eq("Dr. John Doe"),
                eq("john.doe@hospital.com"),
                any(String.class),
                eq("Doctor"),
                eq(subscription.getEndDate())
        );
    }
}
