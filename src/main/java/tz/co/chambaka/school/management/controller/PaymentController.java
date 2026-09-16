package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.finance.PaymentResponse;
import tz.co.chambaka.school.management.dto.finance.RecordPaymentRequest;
import tz.co.chambaka.school.management.security.Access;
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
    @PreAuthorize(Access.PAYMENT_RECORD)
    public PaymentResponse record(@CurrentUser UserPrincipal principal, @Valid @RequestBody RecordPaymentRequest request) {
        return financeService.recordPayment(tenantResolver.requireSchoolId(), request, principal.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize(Access.FINANCE_RECORD)
    public PaymentResponse get(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        return financeService.getPayment(tenantResolver.requireSchoolId(), id, principal);
    }

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize(Access.FINANCE_RECORD)
    public List<PaymentResponse> byInvoice(@CurrentUser UserPrincipal principal, @PathVariable Long invoiceId) {
        return financeService.paymentsForInvoice(tenantResolver.requireSchoolId(), invoiceId, principal);
    }

    @GetMapping("/{id}/receipt.pdf")
    @PreAuthorize(Access.FINANCE_RECORD)
    public org.springframework.http.ResponseEntity<byte[]> receipt(
            @CurrentUser UserPrincipal principal, @PathVariable Long id) {
        byte[] body = financeService.receiptPdf(tenantResolver.requireSchoolId(), id, principal);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"receipt-" + id + ".pdf\"")
                .body(body);
    }
}
