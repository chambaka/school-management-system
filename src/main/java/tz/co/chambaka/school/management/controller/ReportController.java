package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.ReportExportService;
import tz.co.chambaka.school.management.service.StudentService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports")
public class ReportController {

    private final ReportExportService reportExportService;
    private final StudentService studentService;
    private final TenantResolver tenantResolver;

    public ReportController(
            ReportExportService reportExportService,
            StudentService studentService,
            TenantResolver tenantResolver
    ) {
        this.reportExportService = reportExportService;
        this.studentService = studentService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/report-card.pdf")
    @PreAuthorize(Access.REPORT_CARD + " or hasRole('STUDENT')")
    public ResponseEntity<byte[]> reportCardPdf(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) Long studentId,
            @RequestParam Long examId
    ) {
        Long id = studentId == null ? studentService.requireByUser(principal.getId()).getId() : studentId;
        byte[] body = reportExportService.reportCardPdf(tenantResolver.requireSchoolId(), id, examId, principal.getRole());
        return file("report-card.pdf", "application/pdf", body);
    }

    @GetMapping("/report-card.csv")
    @PreAuthorize(Access.REPORT_EXPORT)
    public ResponseEntity<byte[]> reportCardCsv(
            @CurrentUser UserPrincipal principal,
            @RequestParam Long studentId,
            @RequestParam Long examId
    ) {
        byte[] body = reportExportService.reportCardCsv(tenantResolver.requireSchoolId(), studentId, examId, principal.getRole());
        return file("report-card.csv", "text/csv", body);
    }

    private static ResponseEntity<byte[]> file(String name, String type, byte[] body) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(body);
    }
}
