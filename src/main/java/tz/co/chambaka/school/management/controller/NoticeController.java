package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.notice.NoticeRequest;
import tz.co.chambaka.school.management.dto.notice.NoticeResponse;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.NoticeService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notices")
@Tag(name = "Notices")
public class NoticeController {

    private final NoticeService noticeService;
    private final TenantResolver tenantResolver;

    public NoticeController(NoticeService noticeService, TenantResolver tenantResolver) {
        this.noticeService = noticeService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    public List<NoticeResponse> list(@CurrentUser UserPrincipal principal) {
        Long schoolId = tenantResolver.requireSchoolId();
        if (principal.getRole().name().equals("ADMIN") || principal.getRole().name().equals("SUPER_ADMIN")) {
            return noticeService.listForAdmin(schoolId);
        }
        return noticeService.listForAudience(schoolId, principal.getRole());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public NoticeResponse create(@CurrentUser UserPrincipal principal, @Valid @RequestBody NoticeRequest request) {
        return noticeService.create(tenantResolver.requireSchoolId(), request, principal.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public NoticeResponse update(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        return noticeService.update(tenantResolver.requireSchoolId(), id, request);
    }
}
