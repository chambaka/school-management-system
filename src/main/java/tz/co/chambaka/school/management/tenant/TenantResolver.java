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
        return principal.getSchoolId();
    }
}
