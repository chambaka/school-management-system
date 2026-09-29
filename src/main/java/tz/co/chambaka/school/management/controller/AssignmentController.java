package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.AssignmentRequest;
import tz.co.chambaka.school.management.dto.academic.AssignmentResponse;
import tz.co.chambaka.school.management.dto.academic.AssignmentSubmissionResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.AssignmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assignments")
@Tag(name = "Assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final TenantResolver tenantResolver;

    public AssignmentController(AssignmentService assignmentService, TenantResolver tenantResolver) {
        this.assignmentService = assignmentService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.ASSIGNMENT)
    public List<AssignmentResponse> list(@CurrentUser UserPrincipal principal) {
        return assignmentService.list(tenantResolver.requireSchoolId(), principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public AssignmentResponse create(@CurrentUser UserPrincipal principal, @Valid @RequestBody AssignmentRequest request) {
        return assignmentService.create(tenantResolver.requireSchoolId(), principal.getId(), request);
    }

    @PostMapping(value = "/{id}/file", consumes = "multipart/form-data")
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public AssignmentResponse attach(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return assignmentService.attach(tenantResolver.requireSchoolId(), principal.getId(), id, file);
    }

    @PostMapping("/{id}/marks")
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public void marks(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam Long studentId,
            @RequestParam BigDecimal marks
    ) {
        assignmentService.submitMarks(tenantResolver.requireSchoolId(), principal.getId(), id, studentId, marks);
    }

    @PostMapping(value = "/{id}/submit", consumes = "multipart/form-data")
    @PreAuthorize(Access.ASSIGNMENT_SUBMIT)
    public AssignmentSubmissionResponse submit(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        return assignmentService.submitWork(tenantResolver.requireSchoolId(), principal.getId(), id, notes, file);
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public List<AssignmentSubmissionResponse> submissions(@PathVariable Long id) {
        return assignmentService.submissions(tenantResolver.requireSchoolId(), id);
    }
}
