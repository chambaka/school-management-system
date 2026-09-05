package tz.co.chambaka.school.management.config;

import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsProperties properties;
    private final TenantService tenantService;

    public SuperAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SmsProperties properties,
            TenantService tenantService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.tenantService = tenantService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(Role.SUPER_ADMIN) == 0) {
            SmsProperties.SuperAdmin admin = properties.superAdmin();
            User user = User.builder()
                    .name(admin.name())
                    .email(admin.email().toLowerCase())
                    .password(passwordEncoder.encode(admin.password()))
                    .role(Role.SUPER_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Created platform SUPER_ADMIN account: {}", admin.email());
        }
        bindSuperAdminToDefaultTenant();
    }

    private void bindSuperAdminToDefaultTenant() {
        if (!properties.singleTenant()) {
            return;
        }
        Tenant tenant = tenantService.ensureDefaultTenant();
        for (User user : userRepository.findByRole(Role.SUPER_ADMIN)) {
            if (user.getTenantId() == null) {
                user.setTenantId(tenant.getId());
                userRepository.save(user);
                log.info("Bound SUPER_ADMIN {} to default tenant {}", user.getEmail(), tenant.getSlug());
            }
        }
    }
}
