package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.finance.GenerateInvoicesRequest;
import tz.co.chambaka.school.management.dto.finance.InvoiceResponse;
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
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<InvoiceResponse> list(Pageable pageable) {
        return financeService.listInvoices(tenantResolver.requireSchoolId(), pageable);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceResponse> generate(@Valid @RequestBody GenerateInvoicesRequest request) {
        return financeService.generateInvoices(tenantResolver.requireSchoolId(), request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PARENT','STUDENT')")
    public InvoiceResponse get(@PathVariable Long id) {
        return financeService.getInvoice(tenantResolver.requireSchoolId(), id);
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','PARENT')")
    public List<InvoiceResponse> byStudent(@PathVariable Long studentId) {
        return financeService.studentInvoices(tenantResolver.requireSchoolId(), studentId);
    }

    @GetMapping("/students/{studentId}/balance")
    @PreAuthorize("hasAnyRole('ADMIN','PARENT')")
    public Map<String, BigDecimal> balance(@PathVariable Long studentId) {
        return Map.of("outstanding", financeService.outstandingBalance(tenantResolver.requireSchoolId(), studentId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public List<InvoiceResponse> mine(@CurrentUser UserPrincipal principal) {
        Long studentId = studentService.requireByUser(principal.getId()).getId();
        return financeService.studentInvoices(tenantResolver.requireSchoolId(), studentId);
    }
}
