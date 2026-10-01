package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.finance.GenerateInvoicesRequest;
import tz.co.chambaka.school.management.dto.finance.InvoiceResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.FinanceService;
import tz.co.chambaka.school.management.service.StudentService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices")
public class InvoiceController {

    private final FinanceService financeService;
    private final StudentService studentService;
    private final TenantResolver tenantResolver;

    public InvoiceController(
            FinanceService financeService,
            StudentService studentService,
            TenantResolver tenantResolver
    ) {
        this.financeService = financeService;
        this.studentService = studentService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.INVOICE_VIEW)
    public PageResponse<InvoiceResponse> list(Pageable pageable) {
        return financeService.listInvoices(tenantResolver.requireSchoolId(), pageable);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.INVOICE_MANAGE)
    public List<InvoiceResponse> generate(@Valid @RequestBody GenerateInvoicesRequest request) {
        return financeService.generateInvoices(tenantResolver.requireSchoolId(), request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Access.FINANCE_RECORD)
    public InvoiceResponse get(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        return financeService.getInvoice(tenantResolver.requireSchoolId(), id, principal);
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize(Access.FINANCE_RECORD)
    public List<InvoiceResponse> byStudent(@CurrentUser UserPrincipal principal, @PathVariable Long studentId) {
        return financeService.studentInvoices(tenantResolver.requireSchoolId(), studentId, principal);
    }

    @GetMapping("/students/{studentId}/balance")
    @PreAuthorize(Access.FINANCE_RECORD)
    public Map<String, BigDecimal> balance(@CurrentUser UserPrincipal principal, @PathVariable Long studentId) {
        return Map.of("outstanding", financeService.outstandingBalance(
                tenantResolver.requireSchoolId(), studentId, principal));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public List<InvoiceResponse> mine(@CurrentUser UserPrincipal principal) {
        Long studentId = studentService.requireByUser(principal.getId()).getId();
        return financeService.studentInvoices(tenantResolver.requireSchoolId(), studentId, principal);
    }

    @GetMapping("/students/{studentId}/ledger")
    @PreAuthorize(Access.FINANCE_RECORD)
    public tz.co.chambaka.school.management.dto.finance.StudentLedgerResponse ledger(
            @CurrentUser UserPrincipal principal, @PathVariable Long studentId) {
        return financeService.ledger(tenantResolver.requireSchoolId(), studentId, principal);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize(Access.INVOICE_MANAGE)
    public InvoiceResponse removeItem(@PathVariable Long id, @PathVariable Long itemId) {
        return financeService.removeInvoiceItem(tenantResolver.requireSchoolId(), id, itemId);
    }

    @PostMapping("/{id}/discount")
    @PreAuthorize(Access.INVOICE_MANAGE)
    public InvoiceResponse discount(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody tz.co.chambaka.school.management.dto.finance.InvoiceDiscountRequest request
    ) {
        return financeService.applyDiscount(tenantResolver.requireSchoolId(), id, request, principal);
    }
}
