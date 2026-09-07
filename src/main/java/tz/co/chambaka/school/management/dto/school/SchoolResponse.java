package tz.co.chambaka.school.management.dto.school;

import tz.co.chambaka.school.management.model.enums.SchoolStatus;

public record SchoolResponse(
        Long id,
        Long tenantId,
        String tenantName,
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
        boolean financeEnabled,
        boolean attendanceEnabled,
        boolean examsEnabled
) {
    public SchoolResponse withTenantName(String organizationName) {
        return new SchoolResponse(
                id, tenantId, organizationName, name, slug, email, phone, address, website, logoUrl, faviconUrl,
                primaryColor, secondaryColor, accentColor, customDomain, timezone, locale, currency, country,
                status, subscriptionPlan, financeEnabled, attendanceEnabled, examsEnabled);
    }
}
