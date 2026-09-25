package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.model.enums.PromotionAction;
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

    @GetMapping("/enrolment-history.csv")
    @PreAuthorize(Access.REPORT_EXPORT)
    public ResponseEntity<byte[]> enrolmentHistoryCsv(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) PromotionAction action
    ) {
        byte[] body = reportExportService.enrolmentHistoryCsv(
                tenantResolver.requireSchoolId(), studentId, academicYearId, action);
        return file("enrolment-history.csv", "text/csv", body);
    }

    @GetMapping("/report-card.xlsx")
    @PreAuthorize(Access.REPORT_EXPORT)
    public ResponseEntity<byte[]> reportCardXlsx(
            @CurrentUser UserPrincipal principal,
            @RequestParam Long studentId,
            @RequestParam Long examId
    ) {
        byte[] body = reportExportService.reportCardXlsx(tenantResolver.requireSchoolId(), studentId, examId, principal.getRole());
        return file("report-card.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", body);
    }

    @GetMapping("/fees/defaulters")
    @PreAuthorize(Access.REPORT_EXPORT)
    public ResponseEntity<byte[]> defaulters(@RequestParam(defaultValue = "csv") String format) {
        return file("defaulters." + extension(format), media(format),
                reportExportService.defaulters(tenantResolver.requireSchoolId(), format));
    }

    @GetMapping("/finance/collections")
    @PreAuthorize(Access.REPORT_EXPORT)
    public ResponseEntity<byte[]> collections(
            @RequestParam(required = false) java.time.Instant from,
            @RequestParam(required = false) java.time.Instant to,
            @RequestParam(defaultValue = "csv") String format
    ) {
        return file("collections." + extension(format), media(format),
                reportExportService.collections(tenantResolver.requireSchoolId(), from, to, format));
    }

    @GetMapping("/merit-list")
    @PreAuthorize(Access.REPORT_EXPORT)
    public ResponseEntity<byte[]> meritList(
            @CurrentUser UserPrincipal principal,
            @RequestParam Long examId,
            @RequestParam(defaultValue = "csv") String format
    ) {
        return file("merit-list." + extension(format), media(format),
                reportExportService.meritList(tenantResolver.requireSchoolId(), examId, principal.getRole(), format));
    }

    @GetMapping("/attendance")
    @PreAuthorize(Access.REPORT_EXPORT)
    public ResponseEntity<byte[]> attendance(
            @RequestParam java.time.LocalDate start,
            @RequestParam java.time.LocalDate end,
            @RequestParam(defaultValue = "csv") String format
    ) {
        return file("attendance." + extension(format), media(format),
                reportExportService.attendanceSummary(tenantResolver.requireSchoolId(), start, end, format));
    }

    @GetMapping("/term-result")
    @PreAuthorize(Access.REPORT_CARD + " or hasRole('STUDENT')")
    public ResponseEntity<byte[]> termResult(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) Long studentId,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long academicTermId,
            @RequestParam(defaultValue = "pdf") String format
    ) {
        Long id = studentId == null ? studentService.requireByUser(principal.getId()).getId() : studentId;
        byte[] body = reportExportService.termResult(
                tenantResolver.requireSchoolId(), id, academicYearId, academicTermId, principal.getRole(), format);
        return file("term-result." + extension(format), media(format), body);
    }

    @GetMapping("/enrolment-history.pdf")
    @PreAuthorize(Access.REPORT_EXPORT)
    public ResponseEntity<byte[]> enrolmentHistoryPdf(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) PromotionAction action
    ) {
        byte[] body = reportExportService.enrolmentHistoryPdf(
                tenantResolver.requireSchoolId(), studentId, academicYearId, action);
        return file("enrolment-history.pdf", "application/pdf", body);
    }

    private static String extension(String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            return "xlsx";
        }
        if ("pdf".equalsIgnoreCase(format)) {
            return "pdf";
        }
        return "csv";
    }

    private static String media(String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if ("pdf".equalsIgnoreCase(format)) {
            return "application/pdf";
        }
        return "text/csv";
    }

    private static ResponseEntity<byte[]> file(String name, String type, byte[] body) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(body);
    }
}
