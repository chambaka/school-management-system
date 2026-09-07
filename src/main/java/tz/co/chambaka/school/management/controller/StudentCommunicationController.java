package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.communication.CreateStudentMessageRequest;
import tz.co.chambaka.school.management.dto.communication.StudentMessageResponse;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.StudentCommunicationService;
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
@RequestMapping("/api/v1")
@Tag(name = "Student communications")
public class StudentCommunicationController {

    private final StudentCommunicationService communicationService;
    private final TenantResolver tenantResolver;

    public StudentCommunicationController(
            StudentCommunicationService communicationService,
            TenantResolver tenantResolver
    ) {
        this.communicationService = communicationService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/students/{studentId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER','PARENT')")
    public List<StudentMessageResponse> list(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long studentId
    ) {
        return communicationService.listForStudent(tenantResolver.requireSchoolId(), studentId, principal);
    }

    @PostMapping("/students/{studentId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER','PARENT')")
    public StudentMessageResponse post(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long studentId,
            @Valid @RequestBody CreateStudentMessageRequest request
    ) {
        return communicationService.post(tenantResolver.requireSchoolId(), studentId, principal, request);
    }

    @GetMapping("/messages/inbox")
    @PreAuthorize("hasRole('PARENT')")
    public List<StudentMessageResponse> inbox(@CurrentUser UserPrincipal principal) {
        return communicationService.inbox(tenantResolver.requireSchoolId(), principal);
    }
}
