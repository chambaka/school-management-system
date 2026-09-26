package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.tenant.CreateOrganizationAdminRequest;
import tz.co.chambaka.school.management.dto.tenant.OrganizationAdminResponse;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.security.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationAdminService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationAdminService.class);

    private final TenantService tenantService;
    private final UserAccountService userAccountService;
    private final UserRepository userRepository;

    public OrganizationAdminService(
            TenantService tenantService,
            UserAccountService userAccountService,
            UserRepository userRepository
    ) {
        this.tenantService = tenantService;
        this.userAccountService = userAccountService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationAdminResponse> list(Long tenantId) {
        tenantService.require(tenantId);
        return userRepository.findByTenantIdAndRoleOrderByNameAsc(tenantId, Role.ORGANIZATION_ADMIN).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrganizationAdminResponse create(Long tenantId, CreateOrganizationAdminRequest request) {
        tenantService.require(tenantId);
        if (request.password() != null && !request.password().isBlank()) {
            PasswordPolicy.requireValid(request.password(), request.email(), request.name());
        }
        User user = userAccountService.createForTenant(
                tenantId, request.name(), request.email(), request.password(), Role.ORGANIZATION_ADMIN, request.phone());
        log.info("Created organization admin userId={} tenantId={}", user.getId(), tenantId);
        return toResponse(user);
    }

    private OrganizationAdminResponse toResponse(User user) {
        return new OrganizationAdminResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.isEnabled());
    }
}
