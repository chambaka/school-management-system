package tz.co.chambaka.school.management.config;

import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
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

    public SuperAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SmsProperties properties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRole(Role.SUPER_ADMIN) > 0) {
            return;
        }
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
}
