package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(Long schoolId, String name, String email, String rawPassword, Role role, String phone) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        User user = User.builder()
                .schoolId(schoolId)
                .name(name)
                .email(email.toLowerCase())
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .phone(phone)
                .enabled(true)
                .build();
        User saved = userRepository.save(user);
        log.info("Created user id={} email={} role={} schoolId={}", saved.getId(), saved.getEmail(), role, schoolId);
        return saved;
    }
}
