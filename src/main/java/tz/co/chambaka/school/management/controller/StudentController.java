package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.ReportCardResponse;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.parent.LinkParentRequest;
import tz.co.chambaka.school.management.dto.parent.StudentParentResponse;
import tz.co.chambaka.school.management.dto.student.CreateStudentRequest;
import tz.co.chambaka.school.management.dto.student.StudentResponse;
import tz.co.chambaka.school.management.dto.student.UpdateStudentRequest;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.GradeService;
import tz.co.chambaka.school.management.service.ParentService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public PageResponse<StudentResponse> list(@RequestParam(required = false) Long classId, Pageable pageable) {
        return studentService.list(tenantResolver.requireSchoolId(), classId, pageable);
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
}
