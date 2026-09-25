package tz.co.chambaka.school.management.dto.tenant;

import tz.co.chambaka.school.management.model.enums.TenantStatus;

public record UpdateTenantRequest(
        String name,
        String email,
        String phone,
        String country,
        String timezone,
        String currency,
        TenantStatus status,
        String subscriptionPlan,
        Integer termsPerYear
) {
    public UpdateTenantRequest(
            String name,
            String email,
            String phone,
            String country,
            String timezone,
            String currency,
            TenantStatus status,
            String subscriptionPlan
    ) {
        this(name, email, phone, country, timezone, currency, status, subscriptionPlan, null);
    }
}
