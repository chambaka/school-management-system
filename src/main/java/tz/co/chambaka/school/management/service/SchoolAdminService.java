package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.admin.CreateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.admin.SchoolAdminResponse;
import tz.co.chambaka.school.management.dto.admin.UpdateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.security.PasswordPolicy;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.sms.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolAdminService {

    private static final Logger log = LoggerFactory.getLogger(SchoolAdminService.class);

    private final UserRepository userRepository;
    private final UserAccountService userAccountService;
    private final CampusService campusService;
    private final SchoolRepository schoolRepository;
    private final TenantService tenantService;

    public SchoolAdminService(
            UserRepository userRepository,
            UserAccountService userAccountService,
            CampusService campusService,
            SchoolRepository schoolRepository,
            TenantService tenantService
    ) {
        this.userRepository = userRepository;
        this.userAccountService = userAccountService;
        this.campusService = campusService;
        this.schoolRepository = schoolRepository;
        this.tenantService = tenantService;
    }

    public void assertCanManage(Long schoolId, UserPrincipal principal) {
        if (principal.getRole() == Role.SUPER_ADMIN) {
            schoolRepository.findById(schoolId).orElseThrow(() -> ResourceNotFoundException.of("School", schoolId));
            return;
        }
        if (principal.getRole() == Role.HEADMASTER) {
            tenantService.requireSchoolInTenant(principal.getTenantId(), schoolId);
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Only the headmaster or platform admin can manage school officers");
    }

    @Transactional(readOnly = true)
    public PageResponse<SchoolAdminResponse> list(Long schoolId, Pageable pageable) {
        return PageResponse.of(userRepository.findBySchoolIdAndRoleIn(schoolId, Role.schoolOfficers(), pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public SchoolAdminResponse get(Long schoolId, Long id) {
        return toResponse(require(schoolId, id));
    }

    @Transactional
    public SchoolAdminResponse create(Long schoolId, CreateSchoolAdminRequest request) {
        PasswordPolicy.requireValid(request.password(), request.email(), request.name());
        User user = userAccountService.create(
                schoolId, request.name(), request.email(), request.password(), officerRole(request.role()), request.phone());
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
        if (request.role() != null) {
            user.setRole(officerRole(request.role()));
        }
        log.info("Updated school admin userId={} schoolId={} enabled={} role={}",
                user.getId(), schoolId, user.isEnabled(), user.getRole());
        return toResponse(user);
    }

    public User require(Long schoolId, Long id) {
        return userRepository.findByIdAndSchoolIdAndRoleIn(id, schoolId, Role.schoolOfficers())
                .orElseThrow(() -> ResourceNotFoundException.of("School officer", id));
    }

    private static Role officerRole(Role requested) {
        Role role = requested == null ? Role.HEADMASTER : requested;
        if (!Role.schoolOfficers().contains(role)) {
            throw new BusinessException("Role must be HEADMASTER, ACADEMIC_MASTER, or ACCOUNTANT");
        }
        return role;
    }

    private SchoolAdminResponse toResponse(User user) {
        return new SchoolAdminResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.isEnabled(),
                user.getRole());
    }
}
