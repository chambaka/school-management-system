package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.MeritRowResponse;
import tz.co.chambaka.school.management.dto.finance.CollectionRowResponse;
import tz.co.chambaka.school.management.dto.finance.DefaulterRowResponse;
import tz.co.chambaka.school.management.dto.academic.ReportCardResponse;
import tz.co.chambaka.school.management.dto.academic.TermReportResponse;
import tz.co.chambaka.school.management.dto.academic.TermResultResponse;
import tz.co.chambaka.school.management.dto.attendance.AttendanceSummaryResponse;
import tz.co.chambaka.school.management.export.SimpleDocuments;
import tz.co.chambaka.school.management.export.XlsxDocuments;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.PromotionAction;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportExportService {

    private final GradeService gradeService;
    private final AttendanceService attendanceService;
    private final FinanceService financeService;
    private final StudentService studentService;
    private final PromotionService promotionService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public ReportExportService(
            GradeService gradeService,
            AttendanceService attendanceService,
            FinanceService financeService,
            StudentService studentService,
            PromotionService promotionService,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository
    ) {
        this.gradeService = gradeService;
        this.attendanceService = attendanceService;
        this.financeService = financeService;
        this.studentService = studentService;
        this.promotionService = promotionService;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    public byte[] reportCardPdf(Long schoolId, Long studentId, Long examId, Role role) {
        ReportCardResponse card = gradeService.reportCard(schoolId, studentId, examId, role);
        List<String> lines = new ArrayList<>();
        lines.add(card.studentName() + " · " + card.admissionNo());
        lines.add(card.className() + " " + (card.sectionName() == null ? "" : card.sectionName()) + " · " + card.examName());
        card.subjects().forEach(s -> lines.add(s.subjectName() + ": " + s.marksObtained() + "/" + s.maxMarks()));
        lines.add("Overall " + card.overallGrade() + " · " + card.percentage() + "% · GPA " + card.gpa()
                + (card.classPosition() == null ? "" : " · Position " + card.classPosition()));
        if (card.termResults() != null && !card.termResults().isEmpty()) {
            lines.add("Term result (midterm + semi + terminal)");
            card.termResults().forEach(row -> lines.add(termLine(row)));
        }
        return SimpleDocuments.pdf("Report Card", lines);
    }

    public byte[] reportCardCsv(Long schoolId, Long studentId, Long examId, Role role) {
        ReportCardResponse card = gradeService.reportCard(schoolId, studentId, examId, role);
        List<List<String>> rows = card.subjects().stream()
                .map(s -> List.of(s.subjectName(), String.valueOf(s.marksObtained()), String.valueOf(s.maxMarks()), s.passed() ? "Yes" : "No"))
                .toList();
        List<List<String>> table = new ArrayList<>(rows);
        if (card.termResults() != null) {
            for (TermResultResponse row : card.termResults()) {
                table.add(List.of(
                        row.subjectName() + " (term)",
                        money(row.terminalResult()),
                        "",
                        blank(row.letterGrade())
                ));
            }
        }
        return SimpleDocuments.csv(List.of("Subject", "Marks", "Max", "Passed"), table);
    }

    public byte[] enrolmentHistoryCsv(Long schoolId, Long studentId, Long academicYearId, PromotionAction action) {
        List<List<String>> rows = promotionService.history(schoolId, studentId, academicYearId, action).stream()
                .map(row -> List.of(
                        row.effectiveDate() == null ? "" : row.effectiveDate().toString(),
                        row.action() == null ? "" : row.action().name(),
                        blank(row.admissionNo()),
                        blank(row.studentName()),
                        blank(row.academicYearName()),
                        blank(row.schoolClassName()),
                        blank(row.sectionName()),
                        blank(row.notes())
                ))
                .toList();
        return SimpleDocuments.csv(
                List.of("Date", "Action", "Admission No", "Student", "Year", "Class", "Section", "Notes"),
                rows);
    }

    public byte[] enrolmentHistoryPdf(Long schoolId, Long studentId, Long academicYearId, PromotionAction action) {
        List<String> lines = promotionService.history(schoolId, studentId, academicYearId, action).stream()
                .map(row -> {
                    String date = row.effectiveDate() == null ? "" : row.effectiveDate().toString();
                    String actionName = row.action() == null ? "" : row.action().name();
                    String student = (blank(row.admissionNo()) + " " + blank(row.studentName())).trim();
                    String placement = (blank(row.academicYearName()) + " " + blank(row.schoolClassName())
                            + " " + blank(row.sectionName())).trim();
                    String notes = row.notes() == null || row.notes().isBlank() ? "" : " · " + row.notes();
                    return date + " · " + actionName + " · " + student + " · " + placement + notes;
                })
                .toList();
        if (lines.isEmpty()) {
            lines = List.of("No enrolment history for the selected filters.");
        }
        return SimpleDocuments.pdf("Enrolment history", lines);
    }

    public byte[] reportCardXlsx(Long schoolId, Long studentId, Long examId, Role role) {
        ReportCardResponse card = gradeService.reportCard(schoolId, studentId, examId, role);
        List<List<String>> rows = new ArrayList<>(card.subjects().stream()
                .map(s -> List.of(s.subjectName(), String.valueOf(s.marksObtained()), String.valueOf(s.maxMarks()), s.passed() ? "Yes" : "No"))
                .toList());
        if (card.termResults() != null) {
            for (TermResultResponse row : card.termResults()) {
                rows.add(List.of(
                        row.subjectName() + " (term)",
                        money(row.terminalResult()),
                        "",
                        blank(row.letterGrade())
                ));
            }
        }
        return XlsxDocuments.xlsx("Report card", List.of("Subject", "Marks", "Max", "Passed"), rows);
    }

    public byte[] termResult(Long schoolId, Long studentId, Long academicYearId, Long termId, Role role, String format) {
        if (studentId == null) {
            return termResults(schoolId, academicYearId, termId, role, format);
        }
        TermReportResponse report = gradeService.termReport(schoolId, studentId, academicYearId, termId, role);
        List<String> headers = List.of("Subject", "Midterm", "Semi exam", "Semi result", "Terminal exam", "Term result", "Grade");
        List<List<String>> table = report.subjects().stream()
                .map(row -> List.of(
                        blank(row.subjectName()),
                        money(row.midterm()),
                        money(row.semiTerminalExam()),
                        money(row.semiTerminalResult()),
                        money(row.terminalExam()),
                        money(row.terminalResult()),
                        blank(row.letterGrade())
                ))
                .toList();
        String title = "Term result · " + blank(report.studentName())
                + (report.academicYearName() == null ? "" : " · " + report.academicYearName());
        if (table.isEmpty() && "pdf".equalsIgnoreCase(format)) {
            return SimpleDocuments.pdf(title, List.of("No published exam components for this term result yet."));
        }
        return export(title, headers, table, format);
    }

    private byte[] termResults(Long schoolId, Long academicYearId, Long termId, Role role, String format) {
        List<String> headers = List.of(
                "Student", "Admission", "Class", "Subject", "Midterm", "Semi exam", "Semi result",
                "Terminal exam", "Term result", "Grade");
        List<List<String>> table = new ArrayList<>();
        for (TermReportResponse report : gradeService.termReports(schoolId, academicYearId, termId, role)) {
            if (report.subjects() == null || report.subjects().isEmpty()) {
                table.add(List.of(
                        blank(report.studentName()), blank(report.admissionNo()), blank(report.className()),
                        "", "", "", "", "", "", blank(report.overallGrade())));
                continue;
            }
            for (TermResultResponse row : report.subjects()) {
                table.add(List.of(
                        blank(report.studentName()),
                        blank(report.admissionNo()),
                        blank(report.className()),
                        blank(row.subjectName()),
                        money(row.midterm()),
                        money(row.semiTerminalExam()),
                        money(row.semiTerminalResult()),
                        money(row.terminalExam()),
                        money(row.terminalResult()),
                        blank(row.letterGrade())
                ));
            }
        }
        if (table.isEmpty() && "pdf".equalsIgnoreCase(format)) {
            return SimpleDocuments.pdf("Term results", List.of("No students to show."));
        }
        return export("Term results", headers, table, format);
    }

    public List<DefaulterRowResponse> defaulterRows(Long schoolId) {
        return invoiceRepository.findBySchoolIdAndStatusIn(
                        schoolId, List.of(InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE))
                .stream()
                .filter(invoice -> invoice.getBalance().signum() > 0)
                .map(invoice -> new DefaulterRowResponse(
                        invoice.getInvoiceNumber(),
                        invoice.getStudent().getUser().getName(),
                        invoice.getTotalAmount(),
                        invoice.getPaidAmount(),
                        invoice.getDiscountAmount(),
                        invoice.getBalance(),
                        invoice.getStatus().name()
                ))
                .toList();
    }

    public byte[] defaulters(Long schoolId, String format) {
        List<String> headers = List.of("Invoice", "Student", "Total", "Paid", "Discount", "Balance", "Status");
        List<List<String>> table = defaulterRows(schoolId).stream()
                .map(row -> List.of(
                        blank(row.invoiceNumber()),
                        blank(row.studentName()),
                        money(row.total()),
                        money(row.paid()),
                        money(row.discount()),
                        money(row.balance()),
                        blank(row.status())
                ))
                .toList();
        return export("Fee defaulters", headers, table, format);
    }

    public List<CollectionRowResponse> collectionRows(Long schoolId, Instant from, Instant to) {
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now() : to;
        return paymentRepository.findBySchoolIdAndPaidAtBetween(schoolId, start, end).stream()
                .map(payment -> new CollectionRowResponse(
                        payment.getReceiptNumber(),
                        payment.getStudent().getUser().getName(),
                        payment.getAmount(),
                        payment.getMethod().name(),
                        payment.getPaidAt()
                ))
                .toList();
    }

    public byte[] collections(Long schoolId, Instant from, Instant to, String format) {
        List<List<String>> table = collectionRows(schoolId, from, to).stream()
                .map(row -> List.of(
                        blank(row.receiptNumber()),
                        blank(row.studentName()),
                        money(row.amount()),
                        blank(row.method()),
                        row.paidAt() == null ? "" : row.paidAt().toString()
                ))
                .toList();
        return export("Collections", List.of("Receipt", "Student", "Amount", "Method", "Paid at"), table, format);
    }

    public List<MeritRowResponse> meritRows(Long schoolId, Long examId, Role role) {
        return gradeService.meritList(schoolId, examId, role);
    }

    public List<AttendanceSummaryResponse> attendanceRows(Long schoolId, LocalDate start, LocalDate end) {
        return attendanceService.schoolSummaries(schoolId, start, end);
    }

    public byte[] meritList(Long schoolId, Long examId, Role role, String format) {
        List<MeritRowResponse> rows = meritRows(schoolId, examId, role);
        List<List<String>> table = rows.stream()
                .map(row -> List.of(String.valueOf(row.position()), blank(row.admissionNo()), blank(row.studentName()),
                        money(row.total()), money(row.percentage()), blank(row.grade())))
                .toList();
        return export("Merit list", List.of("Position", "Admission", "Student", "Total", "Percent", "Grade"), table, format);
    }

    public byte[] attendanceSummary(Long schoolId, LocalDate start, LocalDate end, String format) {
        List<AttendanceSummaryResponse> rows = attendanceRows(schoolId, start, end);
        List<List<String>> table = rows.stream()
                .map(row -> List.of(blank(row.name()), String.valueOf(row.present()), String.valueOf(row.absent()),
                        String.valueOf(row.late()), String.valueOf(row.excused()), String.valueOf(row.total()),
                        String.valueOf(row.attendancePercent())))
                .toList();
        return export("Attendance", List.of("Student", "Present", "Absent", "Late", "Excused", "Total", "Percent"), table, format);
    }

    private byte[] export(String title, List<String> headers, List<List<String>> rows, String format) {
        if ("xlsx".equalsIgnoreCase(format)) {
            return XlsxDocuments.xlsx(title, headers, rows);
        }
        if ("pdf".equalsIgnoreCase(format)) {
            List<String> lines = new ArrayList<>();
            lines.add(String.join(" | ", headers));
            rows.forEach(row -> lines.add(String.join(" | ", row)));
            if (lines.size() == 1) {
                lines.add("No rows.");
            }
            return SimpleDocuments.pdf(title, lines);
        }
        return SimpleDocuments.csv(headers, rows);
    }

    private static String money(java.math.BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }

    private static String termLine(TermResultResponse row) {
        return blank(row.subjectName())
                + ": midterm " + money(row.midterm())
                + " · semi " + money(row.semiTerminalExam())
                + " → " + money(row.semiTerminalResult())
                + " · terminal " + money(row.terminalExam())
                + " → " + money(row.terminalResult())
                + " " + blank(row.letterGrade());
    }
}
