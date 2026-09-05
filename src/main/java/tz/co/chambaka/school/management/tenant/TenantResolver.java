package tz.co.chambaka.school.management.tenant;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TenantResolver {

    private final SmsProperties smsProperties;
    private final TenantRepository tenantRepository;

    public TenantResolver(SmsProperties smsProperties, TenantRepository tenantRepository) {
        this.smsProperties = smsProperties;
        this.tenantRepository = tenantRepository;
    }

    public Long requireSchoolId() {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "School context is required");
        }
        return schoolId;
    }

    public Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        if (smsProperties.singleTenant()) {
            return tenantRepository.findBySlug(smsProperties.tenancy().defaultTenantSlug())
                    .map(Tenant::getId)
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Tenant context is required"));
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Tenant context is required");
    }

    public Long resolve(Long requestedSchoolId, UserPrincipal principal) {
        if (principal.getRole() == Role.SUPER_ADMIN) {
            if (requestedSchoolId != null) {
                return requestedSchoolId;
            }
            Long fromToken = TenantContext.getSchoolId();
            if (fromToken != null) {
                return fromToken;
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "schoolId is required for platform administrators");
        }
        if (principal.getRole() == Role.TENANT_ADMIN && requestedSchoolId != null) {
            return requestedSchoolId;
        }
        if (principal.getSchoolId() != null) {
            return principal.getSchoolId();
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "School context is required");
    }
}
