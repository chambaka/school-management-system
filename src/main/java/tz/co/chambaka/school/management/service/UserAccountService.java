package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.security.PasswordPolicy;
import tz.co.chambaka.school.management.sms.CredentialSmsService;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private static final List<String> CLEAR_SESSIONS = List.of(
            "delete from RefreshToken r where r.user.id = :id",
            "delete from PasswordResetToken t where t.user.id = :id",
            "delete from TwoFactorChallenge c where c.user.id = :id",
            "delete from PushSubscription p where p.user.id = :id",
            "delete from NotificationPreference n where n.user.id = :id"
    );

    private static final List<String> DETACH_THEN_DELETE = List.of(
            "update Student s set s.user = null where s.user.id in :ids",
            "update Teacher t set t.user = null where t.user.id in :ids",
            "update Parent p set p.user = null where p.user.id in :ids",
            "update StudentCommunication c set c.author = null where c.author.id in :ids",
            "update Payment p set p.recordedBy = null where p.recordedBy.id in :ids",
            "update Notice n set n.createdBy = null where n.createdBy.id in :ids",
            "update StudentAttendance a set a.markedBy = null where a.markedBy.id in :ids",
            "update TeacherAttendance a set a.markedBy = null where a.markedBy.id in :ids",
            "delete from RefreshToken r where r.user.id in :ids",
            "delete from PasswordResetToken t where t.user.id in :ids"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SchoolRepository schoolRepository;
    private final EntityManager entityManager;
    private final CredentialSmsService credentialSms;

    public UserAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SchoolRepository schoolRepository,
            EntityManager entityManager,
            CredentialSmsService credentialSms
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.schoolRepository = schoolRepository;
        this.entityManager = entityManager;
        this.credentialSms = credentialSms;
    }

    public User create(Long schoolId, String name, String email, String rawPassword, Role role, String phone) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        String persistedPhone = PhoneNumbers.persist(phone);
        boolean generate = rawPassword == null || rawPassword.isBlank();
        if (generate && persistedPhone == null) {
            throw new BusinessException("Add a phone number to send a temporary password, or set a password.");
        }
        String password = generate ? PasswordPolicy.generateTemporary() : rawPassword;
        Long tenantId = schoolId == null ? null : schoolRepository.findById(schoolId)
                .map(School::getTenantId)
                .orElse(null);
        User user = User.builder()
                .tenantId(tenantId)
                .schoolId(schoolId)
                .name(name)
                .email(email.toLowerCase())
                .username(uniqueUsername(email))
                .password(passwordEncoder.encode(password))
                .role(role)
                .phone(persistedPhone)
                .enabled(true)
                .build();
        User saved = userRepository.save(user);
        log.info("Created user id={} email={} role={} tenantId={} schoolId={} phone={} tempPasswordGenerated={}",
                saved.getId(), saved.getEmail(), role, tenantId, schoolId, persistedPhone, generate);
        credentialSms.sendTemporaryPassword(saved, password);
        return saved;
    }

    @Transactional
    public User createForTenant(Long tenantId, String name, String email, String rawPassword, Role role, String phone) {
        User saved = create(null, name, email, rawPassword, role, phone);
        saved.setTenantId(tenantId);
        return userRepository.save(saved);
    }

    private String uniqueUsername(String email) {
        String base = email.contains("@") ? email.substring(0, email.indexOf('@')).toLowerCase().replaceAll("[^a-z0-9]", "") : "user";
        if (base.isBlank()) {
            base = "user";
        }
        String candidate = base;
        int n = 1;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = base + n++;
        }
        return candidate;
    }

    @Transactional
    public void setEnabled(User user, boolean enabled) {
        user.setEnabled(enabled);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        if (!enabled) {
            executeById(CLEAR_SESSIONS, user.getId());
        }
        log.info("{} login userId={} email={}", enabled ? "Enabled" : "Disabled", user.getId(), user.getEmail());
    }

    @Transactional
    public void deleteCompletely(User user) {
        Long id = user.getId();
        executeById(CLEAR_SESSIONS, id);
        executeByIds(DETACH_THEN_DELETE, List.of(id));
        userRepository.delete(user);
        log.info("Deleted login userId={} email={}", id, user.getEmail());
    }

    private void executeById(List<String> statements, Long id) {
        for (String jpql : statements) {
            Query query = entityManager.createQuery(jpql);
            query.setParameter("id", id);
            query.executeUpdate();
        }
    }

    private void executeByIds(List<String> statements, List<Long> ids) {
        for (String jpql : statements) {
            Query query = entityManager.createQuery(jpql);
            query.setParameter("ids", ids);
            query.executeUpdate();
        }
    }

    @Transactional
    public void deleteForSchool(Long schoolId) {
        deleteAccounts(userRepository.findBySchoolId(schoolId));
    }

    @Transactional
    public void deleteForTenant(Long tenantId, Collection<Long> schoolIds) {
        List<User> users = new ArrayList<>(userRepository.findByTenantId(tenantId));
        if (schoolIds != null && !schoolIds.isEmpty()) {
            users.addAll(userRepository.findBySchoolIdIn(schoolIds));
        }
        deleteAccounts(users);
    }

    private void deleteAccounts(List<User> users) {
        Map<Long, User> unique = new LinkedHashMap<>();
        for (User user : users) {
            if (user.getId() != null && user.getRole() != Role.SUPER_ADMIN) {
                unique.putIfAbsent(user.getId(), user);
            }
        }
        List<Long> ids = List.copyOf(unique.keySet());
        if (ids.isEmpty()) {
            return;
        }
        executeByIds(DETACH_THEN_DELETE, ids);
        userRepository.deleteAllById(ids);
        log.info("Deleted {} user accounts from live", ids.size());
    }
}
