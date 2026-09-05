package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.parent.CreateParentRequest;
import tz.co.chambaka.school.management.dto.parent.ParentResponse;
import tz.co.chambaka.school.management.dto.parent.StudentParentResponse;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.ParentService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parents")
@Tag(name = "Parents")
public class ParentController {

    private final ParentService parentService;
    private final TenantResolver tenantResolver;

    public ParentController(ParentService parentService, TenantResolver tenantResolver) {
        this.parentService = parentService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public PageResponse<ParentResponse> list(Pageable pageable) {
        return parentService.list(tenantResolver.requireSchoolId(), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public ParentResponse create(@Valid @RequestBody CreateParentRequest request) {
        return parentService.create(tenantResolver.requireSchoolId(), request);
    }

    @GetMapping("/me/children")
    @PreAuthorize("hasRole('PARENT')")
    public List<StudentParentResponse> myChildren(@CurrentUser UserPrincipal principal) {
        return parentService.listByParent(parentService.requireByUser(principal.getId()).getId());
    }
}
