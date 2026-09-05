package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.dto.publicapi.PublicConfigResponse;
import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.repository.TenantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public config")
public class PublicConfigController {

    private final SmsProperties smsProperties;
    private final TenantRepository tenantRepository;

    public PublicConfigController(SmsProperties smsProperties, TenantRepository tenantRepository) {
        this.smsProperties = smsProperties;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/config")
    @Operation(summary = "Deployment mode used by the frontend")
    public PublicConfigResponse config() {
        boolean single = smsProperties.singleTenant();
        String organizationName = null;
        if (single) {
            SmsProperties.Tenancy tenancy = smsProperties.tenancy();
            organizationName = tenantRepository.findBySlug(tenancy.defaultTenantSlug())
                    .map(Tenant::getName)
                    .orElse(tenancy.defaultTenantName());
        }
        return new PublicConfigResponse(single ? "single" : "multi", !single, organizationName);
    }
}
