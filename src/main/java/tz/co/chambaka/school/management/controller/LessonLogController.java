package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.LessonLogRequest;
import tz.co.chambaka.school.management.dto.academic.LessonLogResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.LessonLogService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lesson-logs")
@Tag(name = "Lesson logs")
public class LessonLogController {

    private final LessonLogService lessonLogService;
    private final TenantResolver tenantResolver;

    public LessonLogController(LessonLogService lessonLogService, TenantResolver tenantResolver) {
        this.lessonLogService = lessonLogService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.LESSON_LOG)
    public List<LessonLogResponse> list(
            @RequestParam Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return lessonLogService.bySection(tenantResolver.requireSchoolId(), sectionId, date);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.LESSON_LOG)
    public LessonLogResponse create(@CurrentUser UserPrincipal principal, @Valid @RequestBody LessonLogRequest request) {
        return lessonLogService.create(tenantResolver.requireSchoolId(), principal.getId(), request);
    }
}
