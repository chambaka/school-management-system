package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.finance.FeeStructureRequest;
import tz.co.chambaka.school.management.dto.finance.FeeStructureResponse;
import tz.co.chambaka.school.management.service.FinanceService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fees")
@Tag(name = "Fee structures")
public class FeeController {

    private final FinanceService financeService;
    private final TenantResolver tenantResolver;

    public FeeController(FinanceService financeService, TenantResolver tenantResolver) {
        this.financeService = financeService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<FeeStructureResponse> list(@RequestParam Long academicYearId) {
        return financeService.listFees(tenantResolver.requireSchoolId(), academicYearId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public FeeStructureResponse create(@Valid @RequestBody FeeStructureRequest request) {
        return financeService.createFee(tenantResolver.requireSchoolId(), request);
    }
}
