package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.ExamRequest;
import tz.co.chambaka.school.management.dto.academic.ExamResponse;
import tz.co.chambaka.school.management.dto.academic.ExamSeatResponse;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectRequest;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.ExamService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    @PreAuthorize(Access.ACADEMIC_VIEW)
    public List<ExamResponse> list(@CurrentUser UserPrincipal principal, @RequestParam(required = false) Long academicYearId) {
        return examService.list(tenantResolver.requireSchoolId(), academicYearId, principal.getRole());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.EXAM_MANAGE)
    public ExamResponse create(@Valid @RequestBody ExamRequest request) {
        return examService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.EXAM_MANAGE)
    public ExamResponse update(@PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        return examService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize(Access.GRADE_ENTER)
    public ExamResponse submit(@PathVariable Long id) {
        return examService.submit(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize(Access.EXAM_VERIFY)
    public ExamResponse verify(@PathVariable Long id) {
        return examService.verify(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Access.EXAM_APPROVE)
    public ExamResponse approve(@PathVariable Long id) {
        return examService.approve(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(Access.EXAM_REJECT)
    public ExamResponse reject(@PathVariable Long id, @RequestParam(required = false) String note) {
        return examService.reject(tenantResolver.requireSchoolId(), id, note);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize(Access.EXAM_MANAGE)
    public ExamResponse publish(@PathVariable Long id, @RequestParam boolean published) {
        return examService.publish(tenantResolver.requireSchoolId(), id, published);
    }

    @GetMapping("/{id}/subjects")
    @PreAuthorize(Access.GRADE_ENTER)
    public List<ExamSubjectResponse> subjects(@PathVariable Long id) {
        return examService.listSubjects(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping("/{id}/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.GRADE_ENTER)
    public ExamSubjectResponse addSubject(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ExamSubjectRequest request
    ) {
        return examService.addSubject(tenantResolver.requireSchoolId(), id, request, principal.getId());
    }

    @PutMapping("/{id}/subjects/{paperId}")
    @PreAuthorize(Access.GRADE_ENTER)
    public ExamSubjectResponse updateSubject(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long paperId,
            @Valid @RequestBody ExamSubjectRequest request
    ) {
        return examService.updateSubject(tenantResolver.requireSchoolId(), id, paperId, request, principal.getId());
    }

    @DeleteMapping("/{id}/subjects/{paperId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.GRADE_ENTER)
    public void deleteSubject(
            @CurrentUser UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long paperId
    ) {
        examService.deleteSubject(tenantResolver.requireSchoolId(), id, paperId, principal.getId());
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize(Access.EXAM_MANAGE)
    public List<ExamSubjectResponse> generateSchedule(@PathVariable Long id) {
        return examService.generateSchedule(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping("/{id}/schedule/lock")
    @PreAuthorize(Access.EXAM_MANAGE)
    public ExamResponse lockSchedule(@PathVariable Long id, @RequestParam boolean locked) {
        return examService.lockSchedule(tenantResolver.requireSchoolId(), id, locked);
    }

    @GetMapping("/subjects/{examSubjectId}/seats")
    @PreAuthorize(Access.EXAM_INVIGILATE)
    public List<ExamSeatResponse> seats(@PathVariable Long examSubjectId) {
        return examService.seats(tenantResolver.requireSchoolId(), examSubjectId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.EXAM_MANAGE)
    public void delete(@PathVariable Long id) {
        examService.delete(tenantResolver.requireSchoolId(), id);
    }
}
