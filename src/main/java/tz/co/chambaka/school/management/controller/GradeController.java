package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.GradeImportResponse;
import tz.co.chambaka.school.management.dto.academic.GradeRequest;
import tz.co.chambaka.school.management.dto.academic.GradeResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.GradeImportService;
import tz.co.chambaka.school.management.service.GradeService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/grades")
@Tag(name = "Grades")
public class GradeController {

    private final GradeService gradeService;
    private final GradeImportService gradeImportService;
    private final TenantResolver tenantResolver;

    public GradeController(GradeService gradeService, GradeImportService gradeImportService, TenantResolver tenantResolver) {
        this.gradeService = gradeService;
        this.gradeImportService = gradeImportService;
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

    @GetMapping(value = "/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize(Access.GRADE_ENTER)
    public ResponseEntity<byte[]> template(@RequestParam Long examId, @RequestParam Long subjectId) {
        byte[] body = gradeImportService.template(tenantResolver.requireSchoolId(), examId, subjectId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"grades-template.xlsx\"")
                .body(body);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Access.GRADE_ENTER)
    public GradeImportResponse importMarks(
            @CurrentUser UserPrincipal principal,
            @RequestParam Long examId,
            @RequestParam Long subjectId,
            @RequestParam("file") MultipartFile file
    ) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (name.endsWith(".xls") && !name.endsWith(".xlsx")) {
            throw new BusinessException("Save the file as .xlsx. Download the template from Grades.");
        }
        try {
            return gradeImportService.importMarks(
                    tenantResolver.requireSchoolId(), examId, subjectId, file.getBytes(), principal.getId());
        } catch (IOException ex) {
            throw new BusinessException("Could not read that Excel file.");
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.GRADE_ENTER)
    public GradeResponse record(@CurrentUser UserPrincipal principal, @Valid @RequestBody GradeRequest request) {
        return gradeService.record(tenantResolver.requireSchoolId(), request, principal.getId());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.GRADE_ENTER)
    public void delete(
            @RequestParam Long examId,
            @RequestParam Long studentId,
            @RequestParam Long subjectId
    ) {
        gradeService.delete(tenantResolver.requireSchoolId(), examId, studentId, subjectId);
    }
}
