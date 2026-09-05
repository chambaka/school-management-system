package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.ExamRequest;
import tz.co.chambaka.school.management.dto.academic.ExamResponse;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectRequest;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectResponse;
import tz.co.chambaka.school.management.service.ExamService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exams")
public class ExamController {

    private final ExamService examService;
    private final TenantResolver tenantResolver;

    public ExamController(ExamService examService, TenantResolver tenantResolver) {
        this.examService = examService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT','PARENT')")
    public List<ExamResponse> list(@RequestParam(required = false) Long academicYearId) {
        return examService.list(tenantResolver.requireSchoolId(), academicYearId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ExamResponse create(@Valid @RequestBody ExamRequest request) {
        return examService.create(tenantResolver.requireSchoolId(), request);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ExamResponse publish(@PathVariable Long id, @RequestParam boolean published) {
        return examService.publish(tenantResolver.requireSchoolId(), id, published);
    }

    @GetMapping("/{id}/subjects")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<ExamSubjectResponse> subjects(@PathVariable Long id) {
        return examService.listSubjects(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping("/{id}/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ExamSubjectResponse addSubject(@PathVariable Long id, @Valid @RequestBody ExamSubjectRequest request) {
        return examService.addSubject(tenantResolver.requireSchoolId(), id, request);
    }
}
