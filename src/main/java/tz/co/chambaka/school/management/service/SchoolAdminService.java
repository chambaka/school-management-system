package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.admin.CreateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.admin.SchoolAdminResponse;
import tz.co.chambaka.school.management.dto.admin.UpdateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.security.PasswordPolicy;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolAdminService {

    private static final Logger log = LoggerFactory.getLogger(SchoolAdminService.class);

    private final UserRepository userRepository;
    private final UserAccountService userAccountService;
    private final CampusService campusService;

    public SchoolAdminService(
            UserRepository userRepository,
            UserAccountService userAccountService,
            CampusService campusService
    ) {
        this.userRepository = userRepository;
        this.userAccountService = userAccountService;
        this.campusService = campusService;
    }

    @Transactional(readOnly = true)
    public PageResponse<SchoolAdminResponse> list(Long schoolId, Pageable pageable) {
        return PageResponse.of(userRepository.findBySchoolIdAndRole(schoolId, Role.ADMIN, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public SchoolAdminResponse get(Long schoolId, Long id) {
        return toResponse(require(schoolId, id));
    }

    @Transactional
    public SchoolAdminResponse create(Long schoolId, CreateSchoolAdminRequest request) {
        PasswordPolicy.requireValid(request.password(), request.email(), request.name());
        User user = userAccountService.create(
                schoolId, request.name(), request.email(), request.password(), Role.ADMIN, request.phone());
        user.setCampusId(campusService.requirePrimary(schoolId).getId());
        log.info("Created school admin userId={} schoolId={}", user.getId(), schoolId);
        return toResponse(user);
    }

    @Transactional
    public SchoolAdminResponse update(Long schoolId, Long id, UpdateSchoolAdminRequest request) {
        User user = require(schoolId, id);
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phone() != null) {
            user.setPhone(PhoneNumbers.persist(request.phone()));
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        log.info("Updated school admin userId={} schoolId={} enabled={}", user.getId(), schoolId, user.isEnabled());
        return toResponse(user);
    }

    public User require(Long schoolId, Long id) {
        return userRepository.findByIdAndSchoolIdAndRole(id, schoolId, Role.ADMIN)
                .orElseThrow(() -> ResourceNotFoundException.of("School admin", id));
    }

    private SchoolAdminResponse toResponse(User user) {
        return new SchoolAdminResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.isEnabled());
    }
}
