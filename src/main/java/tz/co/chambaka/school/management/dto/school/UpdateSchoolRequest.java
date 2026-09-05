package tz.co.chambaka.school.management.dto.school;

import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import jakarta.validation.constraints.Size;

public record UpdateSchoolRequest(
        @Size(max = 150) String name,
        String email,
        String phone,
        String address,
        String website,
        String logoUrl,
        String faviconUrl,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String customDomain,
        String timezone,
        String locale,
        String currency,
        String country,
        SchoolStatus status,
        String subscriptionPlan,
        Boolean financeEnabled,
        Boolean attendanceEnabled,
        Boolean examsEnabled
) {
}
