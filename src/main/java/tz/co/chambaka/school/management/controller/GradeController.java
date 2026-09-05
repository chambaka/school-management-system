package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.GradeRequest;
import tz.co.chambaka.school.management.dto.academic.GradeResponse;
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
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<GradeResponse> byExam(@RequestParam Long examId) {
        return gradeService.byExam(tenantResolver.requireSchoolId(), examId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public GradeResponse record(@CurrentUser UserPrincipal principal, @Valid @RequestBody GradeRequest request) {
        return gradeService.record(tenantResolver.requireSchoolId(), request, principal.getId());
    }
}
