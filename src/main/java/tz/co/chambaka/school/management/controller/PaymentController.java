package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.finance.PaymentResponse;
import tz.co.chambaka.school.management.dto.finance.RecordPaymentRequest;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.FinanceService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments")
public class PaymentController {

    private final FinanceService financeService;
    private final TenantResolver tenantResolver;

    public PaymentController(FinanceService financeService, TenantResolver tenantResolver) {
        this.financeService = financeService;
        this.tenantResolver = tenantResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public PaymentResponse record(@CurrentUser UserPrincipal principal, @Valid @RequestBody RecordPaymentRequest request) {
        return financeService.recordPayment(tenantResolver.requireSchoolId(), request, principal.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','PARENT','STUDENT')")
    public PaymentResponse get(@PathVariable Long id) {
        return financeService.getPayment(tenantResolver.requireSchoolId(), id);
    }

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','PARENT','STUDENT')")
    public List<PaymentResponse> byInvoice(@PathVariable Long invoiceId) {
        return financeService.paymentsForInvoice(tenantResolver.requireSchoolId(), invoiceId);
    }
}
