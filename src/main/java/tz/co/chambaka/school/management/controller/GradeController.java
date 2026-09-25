package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.GradeRequest;
import tz.co.chambaka.school.management.dto.academic.GradeResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.GradeService;
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
@RequestMapping("/api/v1/grades")
@Tag(name = "Grades")
public class GradeController {

    private final GradeService gradeService;
    private final TenantResolver tenantResolver;

    public GradeController(GradeService gradeService, TenantResolver tenantResolver) {
        this.gradeService = gradeService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.GRADE_ENTER)
    public List<GradeResponse> byExam(@RequestParam Long examId) {
        return gradeService.byExam(tenantResolver.requireSchoolId(), examId);
    }

    @GetMapping("/grid")
    @PreAuthorize(Access.GRADE_ENTER)
    public tz.co.chambaka.school.management.dto.academic.GradeGridResponse grid(
            @RequestParam Long examId, @RequestParam Long subjectId) {
        return gradeService.grid(tenantResolver.requireSchoolId(), examId, subjectId);
    }

    @PostMapping("/bulk")
    @PreAuthorize(Access.GRADE_ENTER)
    public List<GradeResponse> bulk(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody tz.co.chambaka.school.management.dto.academic.BulkGradeRequest request
    ) {
        return gradeService.recordBulk(tenantResolver.requireSchoolId(), request, principal.getId());
    }

    @GetMapping("/term-result")
    @PreAuthorize(Access.TERM_RESULT)
    public tz.co.chambaka.school.management.dto.academic.TermResultResponse termResult(
            @CurrentUser UserPrincipal principal,
            @RequestParam Long studentId,
            @RequestParam Long subjectId,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long academicTermId
    ) {
        return gradeService.termResult(
                tenantResolver.requireSchoolId(), studentId, subjectId, academicYearId, academicTermId, principal.getRole());
    }

    @GetMapping("/term-results")
    @PreAuthorize(Access.GRADE_ENTER)
    public java.util.List<tz.co.chambaka.school.management.dto.academic.TermResultResponse> classTermResults(
            @CurrentUser UserPrincipal principal,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long academicTermId,
            @RequestParam Long schoolClassId,
            @RequestParam Long subjectId
    ) {
        return gradeService.classTermResults(
                tenantResolver.requireSchoolId(), academicYearId, academicTermId, schoolClassId, subjectId, principal.getRole());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.GRADE_ENTER)
    public GradeResponse record(@CurrentUser UserPrincipal principal, @Valid @RequestBody GradeRequest request) {
        return gradeService.record(tenantResolver.requireSchoolId(), request, principal.getId());
    }
}
