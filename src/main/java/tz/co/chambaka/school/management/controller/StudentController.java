package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.ReportCardResponse;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.parent.LinkParentRequest;
import tz.co.chambaka.school.management.dto.parent.StudentParentResponse;
import tz.co.chambaka.school.management.dto.student.CreateStudentRequest;
import tz.co.chambaka.school.management.dto.student.StudentResponse;
import tz.co.chambaka.school.management.dto.student.UpdateStudentRequest;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.GradeService;
import tz.co.chambaka.school.management.service.ParentService;
import tz.co.chambaka.school.management.service.StoredPhoto;
import tz.co.chambaka.school.management.service.StudentService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students")
public class StudentController {

    private final StudentService studentService;
    private final ParentService parentService;
    private final GradeService gradeService;
    private final TenantResolver tenantResolver;

    public StudentController(
            StudentService studentService,
            ParentService parentService,
            GradeService gradeService,
            TenantResolver tenantResolver
    ) {
        this.studentService = studentService;
        this.parentService = parentService;
        this.gradeService = gradeService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public PageResponse<StudentResponse> list(
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "false") boolean archived,
            Pageable pageable
    ) {
        return studentService.list(tenantResolver.requireSchoolId(), classId, archived, pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentResponse me(@CurrentUser UserPrincipal principal) {
        return studentService.get(tenantResolver.requireSchoolId(), studentService.requireByUser(principal.getId()).getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public StudentResponse get(@PathVariable Long id) {
        return studentService.get(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public StudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @PostMapping("/{id}/parents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public StudentParentResponse linkParent(@PathVariable Long id, @Valid @RequestBody LinkParentRequest request) {
        return parentService.link(tenantResolver.requireSchoolId(), id, request);
    }

    @DeleteMapping("/{id}/parents/{parentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public void unlinkParent(@PathVariable Long id, @PathVariable Long parentId) {
        parentService.unlink(tenantResolver.requireSchoolId(), id, parentId);
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public StudentResponse suspend(@PathVariable Long id) {
        return studentService.suspend(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public StudentResponse archive(@PathVariable Long id) {
        return studentService.archive(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public StudentResponse restore(@PathVariable Long id) {
        return studentService.restore(tenantResolver.requireSchoolId(), id);
    }

    @GetMapping("/{id}/parents")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<StudentParentResponse> parents(@PathVariable Long id) {
        return parentService.listByStudent(tenantResolver.requireSchoolId(), id);
    }

    @GetMapping("/{id}/report-card")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER','PARENT')")
    public ReportCardResponse reportCard(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam Long examId
    ) {
        if (principal.getRole().name().equals("PARENT")) {
            parentService.assertLinked(principal.getId(), id);
        }
        return gradeService.reportCard(tenantResolver.requireSchoolId(), id, examId);
    }

    @GetMapping("/me/report-card")
    @PreAuthorize("hasRole('STUDENT')")
    public ReportCardResponse myReportCard(@CurrentUser UserPrincipal principal, @RequestParam Long examId) {
        Long studentId = studentService.requireByUser(principal.getId()).getId();
        return gradeService.reportCard(tenantResolver.requireSchoolId(), studentId, examId);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public StudentResponse uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return studentService.uploadPhoto(tenantResolver.requireSchoolId(), id, file);
    }

    @DeleteMapping("/{id}/photo")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public StudentResponse deletePhoto(@PathVariable Long id) {
        return studentService.deletePhoto(tenantResolver.requireSchoolId(), id);
    }

    @GetMapping("/me/photo")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Resource> myPhoto(@CurrentUser UserPrincipal principal) {
        Long studentId = studentService.requireByUser(principal.getId()).getId();
        return toPhotoResponse(studentService.photoFile(tenantResolver.requireSchoolId(), studentId));
    }

    @GetMapping("/{id}/photo")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER','STUDENT','PARENT')")
    public ResponseEntity<Resource> photo(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        authorizePhotoView(principal, id);
        return toPhotoResponse(studentService.photoFile(tenantResolver.requireSchoolId(), id));
    }

    private void authorizePhotoView(UserPrincipal principal, Long studentId) {
        if (principal.getRole() == Role.STUDENT) {
            if (!studentService.requireByUser(principal.getId()).getId().equals(studentId)) {
                throw new AccessDeniedException("Not allowed to view this photo");
            }
        } else if (principal.getRole() == Role.PARENT) {
            parentService.assertLinked(principal.getId(), studentId);
        }
    }

    private static ResponseEntity<Resource> toPhotoResponse(StoredPhoto photo) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                .body(new FileSystemResource(photo.path()));
    }
}
