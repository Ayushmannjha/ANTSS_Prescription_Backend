package com.antss_prescription.configuration;

import com.antss_prescription.entity.SubscriptionPackage;
import com.antss_prescription.entity.User;
import com.antss_prescription.entity.LoginCredential;
import com.antss_prescription.enums.DurationType;
import com.antss_prescription.enums.RegistrationStatus;
import com.antss_prescription.enums.Role;
import com.antss_prescription.enums.UserType;
import com.antss_prescription.enums.LoginStatus;
import com.antss_prescription.repository.PackageRepository;
import com.antss_prescription.repository.UserRepository;
import com.antss_prescription.repository.LoginCredentialRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final LoginCredentialRepository loginCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.default-password}")
    private String adminDefaultPassword;

    public DataInitializer(UserRepository userRepository,
                           PackageRepository packageRepository,
                           LoginCredentialRepository loginCredentialRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.packageRepository = packageRepository;
        this.loginCredentialRepository = loginCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
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
}
