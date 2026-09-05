package tz.co.chambaka.school.management.tenant;

import tz.co.chambaka.school.management.exception.ApiException;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TenantResolver {

    public Long requireSchoolId() {
        Long schoolId = TenantContext.getSchoolId();
        if (schoolId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "School context is required");
        }
        return schoolId;
    }

    public Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tenant context is required");
        }
        return tenantId;
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
