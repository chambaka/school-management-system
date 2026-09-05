package tz.co.chambaka.school.management.dto.school;

import tz.co.chambaka.school.management.model.enums.SchoolStatus;

import java.time.Instant;

public record SchoolResponse(
        Long id,
        Long tenantId,
        String name,
        String slug,
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
        Instant trialEndsAt,
        boolean financeEnabled,
        boolean attendanceEnabled,
        boolean examsEnabled
) {
}
