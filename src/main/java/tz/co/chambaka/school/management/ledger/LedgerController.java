package tz.co.chambaka.school.management.ledger;

import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
@Tag(name = "Ledger")
public class LedgerController {

    private final LedgerService ledgerService;
    private final TenantResolver tenantResolver;

    public LedgerController(LedgerService ledgerService, TenantResolver tenantResolver) {
        this.ledgerService = ledgerService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize(Access.FINANCE_RECORD)
    public List<LedgerEntryResponse> student(@PathVariable Long studentId) {
        return ledgerService.list(tenantResolver.requireSchoolId(), studentId);
    }
}
