package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.dto.admin.CreateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.admin.SchoolAdminResponse;
import tz.co.chambaka.school.management.dto.admin.UpdateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.service.SchoolAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/school-admins")
@Tag(name = "School admins")
public class SchoolAdminController {

    private final SchoolAdminService schoolAdminService;

    public SchoolAdminController(SchoolAdminService schoolAdminService) {
        this.schoolAdminService = schoolAdminService;
    }

    @GetMapping
    @PreAuthorize(Access.ORG_SCHOOLS)
    public PageResponse<SchoolAdminResponse> list(
            @RequestParam Long schoolId,
            @CurrentUser UserPrincipal principal,
            Pageable pageable
    ) {
        schoolAdminService.assertCanManage(schoolId, principal);
        return schoolAdminService.list(schoolId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Access.ORG_SCHOOLS)
    public SchoolAdminResponse get(
            @RequestParam Long schoolId,
            @PathVariable Long id,
            @CurrentUser UserPrincipal principal
    ) {
        schoolAdminService.assertCanManage(schoolId, principal);
        return schoolAdminService.get(schoolId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.ORG_SCHOOLS)
    public SchoolAdminResponse create(
            @RequestParam Long schoolId,
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody CreateSchoolAdminRequest request
    ) {
        schoolAdminService.assertCanManage(schoolId, principal);
        return schoolAdminService.create(schoolId, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.ORG_SCHOOLS)
    public SchoolAdminResponse update(
            @RequestParam Long schoolId,
            @PathVariable Long id,
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody UpdateSchoolAdminRequest request
    ) {
        schoolAdminService.assertCanManage(schoolId, principal);
        return schoolAdminService.update(schoolId, id, request);
    }
}
