package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.attendance.AttendanceSummaryResponse;
import tz.co.chambaka.school.management.dto.attendance.MarkStudentAttendanceRequest;
import tz.co.chambaka.school.management.dto.attendance.MarkTeacherAttendanceRequest;
import tz.co.chambaka.school.management.dto.attendance.StudentAttendanceResponse;
import tz.co.chambaka.school.management.dto.attendance.TeacherAttendanceResponse;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.AttendanceService;
import tz.co.chambaka.school.management.service.StudentService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@Tag(name = "Attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final StudentService studentService;
    private final TenantResolver tenantResolver;

    public AttendanceController(
            AttendanceService attendanceService,
            StudentService studentService,
            TenantResolver tenantResolver
    ) {
        this.attendanceService = attendanceService;
        this.studentService = studentService;
        this.tenantResolver = tenantResolver;
    }

    @PostMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<StudentAttendanceResponse> markStudents(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody MarkStudentAttendanceRequest request
    ) {
        return attendanceService.markStudents(tenantResolver.requireSchoolId(), request, principal.getId());
    }

    @PostMapping("/teachers")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public List<TeacherAttendanceResponse> markTeachers(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody MarkTeacherAttendanceRequest request
    ) {
        return attendanceService.markTeachers(tenantResolver.requireSchoolId(), request, principal.getId());
    }

    @GetMapping("/students/daily")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<StudentAttendanceResponse> dailyStudents(
            @RequestParam Long sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return attendanceService.dailyStudents(tenantResolver.requireSchoolId(), sectionId, date);
    }

    @GetMapping("/teachers/daily")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public List<TeacherAttendanceResponse> dailyTeachers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return attendanceService.dailyTeachers(tenantResolver.requireSchoolId(), date);
    }

    @GetMapping("/students/{studentId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public AttendanceSummaryResponse studentSummary(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return attendanceService.studentSummary(tenantResolver.requireSchoolId(), studentId, start, end);
    }

    @GetMapping("/me/summary")
    @PreAuthorize("hasRole('STUDENT')")
    public AttendanceSummaryResponse mySummary(
            @CurrentUser UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        Long studentId = studentService.requireByUser(principal.getId()).getId();
        return attendanceService.studentSummary(tenantResolver.requireSchoolId(), studentId, start, end);
    }
}
