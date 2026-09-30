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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import tz.co.chambaka.school.management.service.StoredPhoto;

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

    @PutMapping("/{id}")
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public AssignmentResponse update(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request
    ) {
        return assignmentService.update(tenantResolver.requireSchoolId(), principal, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public void delete(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        assignmentService.delete(tenantResolver.requireSchoolId(), principal, id);
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public AssignmentResponse lock(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        return assignmentService.lock(tenantResolver.requireSchoolId(), principal, id);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public AssignmentResponse publish(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        return assignmentService.publish(tenantResolver.requireSchoolId(), principal, id);
    }

    @PostMapping(value = "/{id}/file", consumes = "multipart/form-data")
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public AssignmentResponse attach(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return assignmentService.attach(tenantResolver.requireSchoolId(), principal, id, file);
    }

    @GetMapping("/{id}/file")
    @PreAuthorize(Access.ASSIGNMENT)
    public ResponseEntity<Resource> file(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        return toFileResponse(assignmentService.file(tenantResolver.requireSchoolId(), principal, id));
    }

    @GetMapping("/{id}/files/{fileId}")
    @PreAuthorize(Access.ASSIGNMENT)
    public ResponseEntity<Resource> file(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long fileId
    ) {
        return toFileResponse(assignmentService.file(tenantResolver.requireSchoolId(), principal, id, fileId));
    }

    @DeleteMapping("/{id}/files/{fileId}")
    @PreAuthorize(Access.ASSIGNMENT_WRITE)
    public AssignmentResponse removeFile(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long fileId
    ) {
        return assignmentService.removeFile(tenantResolver.requireSchoolId(), principal, id, fileId);
    }

    @GetMapping("/{id}/submissions/{studentId}/file")
    @PreAuthorize(Access.ASSIGNMENT)
    public ResponseEntity<Resource> submissionFile(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long studentId
    ) {
        return toFileResponse(assignmentService.submissionFile(tenantResolver.requireSchoolId(), principal, id, studentId));
    }

    @GetMapping("/{id}/submissions/{studentId}/attempts/{submissionId}/file")
    @PreAuthorize(Access.ASSIGNMENT)
    public ResponseEntity<Resource> submissionAttemptFile(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long studentId,
            @PathVariable Long submissionId
    ) {
        return toFileResponse(assignmentService.submissionFile(
                tenantResolver.requireSchoolId(), principal, id, studentId, submissionId));
    }

    @GetMapping("/{id}/submissions/{studentId}/attempts/{submissionId}/files/{fileId}")
    @PreAuthorize(Access.ASSIGNMENT)
    public ResponseEntity<Resource> submissionAttemptFile(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long studentId,
            @PathVariable Long submissionId,
            @PathVariable Long fileId
    ) {
        return toFileResponse(assignmentService.submissionFile(
                tenantResolver.requireSchoolId(), principal, id, studentId, submissionId, fileId));
    }

    @PostMapping(value = "/{id}/mine/files", consumes = "multipart/form-data")
    @PreAuthorize(Access.ASSIGNMENT_SUBMIT)
    public AssignmentSubmissionResponse attachMyFile(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return assignmentService.attachMyFile(tenantResolver.requireSchoolId(), principal, id, file);
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

    @GetMapping({"/me/submissions", "/mine/submissions"})
    @PreAuthorize(Access.ASSIGNMENT_SUBMIT)
    public List<AssignmentSubmissionResponse> mySubmissions(@CurrentUser UserPrincipal principal) {
        return assignmentService.mySubmissions(tenantResolver.requireSchoolId(), principal.getId());
    }

    @GetMapping("/{id}/mine")
    @PreAuthorize(Access.ASSIGNMENT_SUBMIT)
    public AssignmentSubmissionResponse mySubmission(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        return assignmentService.mySubmission(tenantResolver.requireSchoolId(), principal, id);
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize(Access.ASSIGNMENT)
    public List<AssignmentSubmissionResponse> submissions(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        return assignmentService.submissions(tenantResolver.requireSchoolId(), principal, id);
    }

    private static ResponseEntity<Resource> toFileResponse(StoredPhoto file) {
        String filename = file.path().getFileName().toString().replace("\"", "");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                .body(new FileSystemResource(file.path()));
    }
}
