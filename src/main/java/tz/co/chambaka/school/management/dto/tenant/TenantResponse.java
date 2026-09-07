package tz.co.chambaka.school.management.dto.tenant;

import tz.co.chambaka.school.management.model.enums.TenantStatus;

public record TenantResponse(
        Long id,
        String name,
        String slug,
        String email,
        String phone,
        String country,
        String timezone,
        String currency,
        TenantStatus status,
        String subscriptionPlan,
        long schoolCount
) {
}
