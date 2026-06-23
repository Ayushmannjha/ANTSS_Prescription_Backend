package com.antss_prescription.configuration;

import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.entity.User;
import com.antss_prescription.entity.LoginCredential;
import com.antss_prescription.entity.LoginSession;
import com.antss_prescription.enums.DurationType;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.enums.Role;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.enums.LoginStatus;
import com.antss_prescription.enums.DiagnosticStatus;
import com.antss_prescription.entity.prescription.DiagnosticOrder;
import com.antss_prescription.entity.prescription.Investigations;
import com.antss_prescription.entity.prescription.TestRequested;
import com.antss_prescription.repository.prescription.DiagnosticOrderRepository;
import com.antss_prescription.repository.prescription.InvestigationsRepo;
import com.antss_prescription.repository.prescription.TestRequestedRepo;
import com.antss_prescription.repository.PackageRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.LoginCredentialRepository;
import com.antss_prescription.repository.UserSubscriptionRepository;
import com.antss_prescription.repository.LoginSessionRepository;
import com.antss_prescription.security.TokenHashService;
import com.antss_prescription.entity.UserSubscription;
import com.antss_prescription.enums.SubscriptionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final LoginCredentialRepository loginCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final TokenHashService tokenHashService;
    private final JdbcTemplate jdbcTemplate;
    private final DiagnosticOrderRepository diagnosticOrderRepository;
    private final InvestigationsRepo investigationsRepo;
    private final TestRequestedRepo testRequestedRepo;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.default-password}")
    private String adminDefaultPassword;

    public DataInitializer(UserRepository userRepository,
                           PackageRepository packageRepository,
                           LoginCredentialRepository loginCredentialRepository,
                           PasswordEncoder passwordEncoder,
                           UserSubscriptionRepository userSubscriptionRepository,
                           LoginSessionRepository loginSessionRepository,
                           TokenHashService tokenHashService,
                           JdbcTemplate jdbcTemplate,
                           DiagnosticOrderRepository diagnosticOrderRepository,
                           InvestigationsRepo investigationsRepo,
                           TestRequestedRepo testRequestedRepo) {
        this.userRepository = userRepository;
        this.packageRepository = packageRepository;
        this.loginCredentialRepository = loginCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.loginSessionRepository = loginSessionRepository;
        this.tokenHashService = tokenHashService;
        this.jdbcTemplate = jdbcTemplate;
        this.diagnosticOrderRepository = diagnosticOrderRepository;
        this.investigationsRepo = investigationsRepo;
        this.testRequestedRepo = testRequestedRepo;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS patient_registration_number_seq "
                + "START WITH 1000000 INCREMENT BY 1");
        normalizeCurrentSubscriptions();
        migrateStoredTokenHashes();
        migrateLegacyDiagnostics();
        if (packageRepository.count() == 0) {
            SubscriptionPackage silver = new SubscriptionPackage();
            silver.setPackageName("Silver Package (6 Months)");
            silver.setDurationType(DurationType.SIX_MONTH);
            silver.setBaseDoctorLimit(3);
            silver.setPackagePrice(BigDecimal.valueOf(15000.00));
            silver.setExtraDoctorPrice(BigDecimal.valueOf(3000.00));
            silver.setFeatures("3 Doctors limit, standard prescription formatting, email summaries");
            silver.setActive(true);
            packageRepository.save(silver);

            SubscriptionPackage gold = new SubscriptionPackage();
            gold.setPackageName("Gold Package (1 Year)");
            gold.setDurationType(DurationType.ONE_YEAR);
            gold.setBaseDoctorLimit(5);
            gold.setPackagePrice(BigDecimal.valueOf(25000.00));
            gold.setExtraDoctorPrice(BigDecimal.valueOf(2500.00));
            gold.setFeatures("5 Doctors limit, pro prescription formatting, email summaries, priority support");
            gold.setActive(true);
            packageRepository.save(gold);

            SubscriptionPackage platinum = new SubscriptionPackage();
            platinum.setPackageName("Platinum Package (2 Years)");
            platinum.setDurationType(DurationType.TWO_YEAR);
            platinum.setBaseDoctorLimit(10);
            platinum.setPackagePrice(BigDecimal.valueOf(45000.00));
            platinum.setExtraDoctorPrice(BigDecimal.valueOf(2000.00));
            platinum.setFeatures("10 Doctors limit, all features unlocked, custom branding, 24/7 dedicated support");
            platinum.setActive(true);
            packageRepository.save(platinum);

            log.info("Default subscription packages seeded successfully.");
        }

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setFullName("System Admin");
            admin.setEmail(adminEmail);
            admin.setMobileNumber("0000000000");
            admin.setUserType(UserType.HOSPITAL);
           // admin.setPassword(passwordEncoder.encode(adminDefaultPassword));
            admin.setStatus(RegistrationStatus.APPROVED);
            admin.setRole(Role.ROLE_ADMIN);

            User savedAdmin = userRepository.save(admin);

            LoginCredential adminCred = new LoginCredential();
            adminCred.setUser(savedAdmin);
            adminCred.setUsername(adminEmail);
            adminCred.setPasswordHash(passwordEncoder.encode(adminDefaultPassword));
            adminCred.setLoginStatus(LoginStatus.ACTIVE);
            loginCredentialRepository.save(adminCred);

            log.info("Default admin user created with email: {}", adminEmail);
        }
    }

    private void normalizeCurrentSubscriptions() {
        Map<UUID, List<UserSubscription>> byUser = userSubscriptionRepository.findAll().stream()
                .filter(sub -> sub.getSubscriptionStatus() == SubscriptionStatus.ACTIVE
                        || sub.getSubscriptionStatus() == SubscriptionStatus.PENDING)
                .collect(Collectors.groupingBy(sub -> sub.getUser().getId()));

        for (Map.Entry<UUID, List<UserSubscription>> entry : byUser.entrySet()) {
            List<UserSubscription> current = entry.getValue().stream()
                    .sorted(Comparator.comparing(UserSubscription::getStartDate).reversed())
                    .toList();
            for (int i = 1; i < current.size(); i++) {
                UserSubscription subscription = current.get(i);
                subscription.setSubscriptionStatus(SubscriptionStatus.EXPIRED);
                subscription.setActiveOwnerKey(null);
                userSubscriptionRepository.save(subscription);
                log.warn("Expired duplicate current subscription {} for user {}",
                        subscription.getId(), entry.getKey());
            }
            userSubscriptionRepository.flush();
            UserSubscription keeper = current.get(0);
            keeper.setActiveOwnerKey(entry.getKey().toString());
            userSubscriptionRepository.save(keeper);
        }
    }

    private void migrateStoredTokenHashes() {
        for (LoginSession session : loginSessionRepository.findAll()) {
            if (session.getToken() != null && !tokenHashService.isHash(session.getToken())) {
                session.setToken(tokenHashService.hash(session.getToken()));
            }
            if (session.getRefreshToken() != null && !tokenHashService.isHash(session.getRefreshToken())) {
                session.setRefreshToken(tokenHashService.hash(session.getRefreshToken()));
            }
            loginSessionRepository.save(session);
        }
        for (User user : userRepository.findAll()) {
            if (user.getPasswordResetToken() != null
                    && !tokenHashService.isHash(user.getPasswordResetToken())) {
                user.setPasswordResetToken(tokenHashService.hash(user.getPasswordResetToken()));
                userRepository.save(user);
            }
        }
    }

    private void migrateLegacyDiagnostics() {
        for (TestRequested legacy : testRequestedRepo.findAll()) {
            migrateLegacyDiagnostic("test:" + legacy.getId(), legacy.getTestName(), legacy.getNotes(),
                    DiagnosticStatus.REQUESTED, legacy.getPatientRegistration(), legacy.getPrescription(),
                    legacy.getDocument(), legacy.getCreateAt(), null);
        }
        for (Investigations legacy : investigationsRepo.findAll()) {
            migrateLegacyDiagnostic("investigation:" + legacy.getId(), legacy.getInestigationName(),
                    legacy.getNotes(), DiagnosticStatus.COMPLETED, legacy.getPatientRegistration(),
                    legacy.getPrescription(), legacy.getDocument(), legacy.getCreateAt(), legacy.getUpdatedAt());
        }
    }

    private void migrateLegacyDiagnostic(String source, String name, String notes,
            DiagnosticStatus status,
            com.antss_prescription.entity.prescription.PatientRegistration registration,
            com.antss_prescription.entity.prescription.Prescription prescription,
            com.antss_prescription.entity.prescription.Document document,
            java.time.LocalDateTime requestedAt, java.time.LocalDateTime completedAt) {
        if (registration == null || diagnosticOrderRepository.findByLegacySource(source).isPresent()) return;
        DiagnosticOrder order = new DiagnosticOrder();
        order.setLegacySource(source);
        order.setTestName(name == null || name.isBlank() ? "Legacy diagnostic" : name);
        order.setNotes(notes);
        order.setStatus(status);
        order.setPatientRegistration(registration);
        order.setPrescription(prescription);
        order.setReportDocument(document);
        order.setRequestedAt(requestedAt);
        if (status == DiagnosticStatus.COMPLETED) {
            order.setStartedAt(requestedAt);
            order.setCompletedAt(completedAt == null ? requestedAt : completedAt);
        }
        diagnosticOrderRepository.save(order);
    }
}
