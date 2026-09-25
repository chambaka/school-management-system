package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.MeritRowResponse;
import tz.co.chambaka.school.management.dto.academic.ReportCardResponse;
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
        return SimpleDocuments.pdf("Report Card", lines);
    }

    public byte[] reportCardCsv(Long schoolId, Long studentId, Long examId, Role role) {
        ReportCardResponse card = gradeService.reportCard(schoolId, studentId, examId, role);
        List<List<String>> rows = card.subjects().stream()
                .map(s -> List.of(s.subjectName(), String.valueOf(s.marksObtained()), String.valueOf(s.maxMarks()), s.passed() ? "Yes" : "No"))
                .toList();
        return SimpleDocuments.csv(List.of("Subject", "Marks", "Max", "Passed"), rows);
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
        List<List<String>> rows = card.subjects().stream()
                .map(s -> List.of(s.subjectName(), String.valueOf(s.marksObtained()), String.valueOf(s.maxMarks()), s.passed() ? "Yes" : "No"))
                .toList();
        return XlsxDocuments.xlsx("Report card", List.of("Subject", "Marks", "Max", "Passed"), rows);
    }

    public byte[] defaulters(Long schoolId, String format) {
        List<String> headers = List.of("Invoice", "Student", "Total", "Paid", "Discount", "Balance", "Status");
        List<List<String>> table = invoiceRepository.findBySchoolIdAndStatusIn(
                        schoolId, List.of(InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE))
                .stream()
                .filter(invoice -> invoice.getBalance().signum() > 0)
                .map(invoice -> List.of(
                        invoice.getInvoiceNumber(),
                        invoice.getStudent().getUser().getName(),
                        money(invoice.getTotalAmount()),
                        money(invoice.getPaidAmount()),
                        money(invoice.getDiscountAmount()),
                        money(invoice.getBalance()),
                        invoice.getStatus().name()
                ))
                .toList();
        return export("Fee defaulters", headers, table, format);
    }

    public byte[] collections(Long schoolId, Instant from, Instant to, String format) {
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now() : to;
        List<List<String>> table = paymentRepository.findBySchoolIdAndPaidAtBetween(schoolId, start, end).stream()
                .map(payment -> List.of(
                        payment.getReceiptNumber(),
                        payment.getStudent().getUser().getName(),
                        money(payment.getAmount()),
                        payment.getMethod().name(),
                        payment.getPaidAt().toString()
                ))
                .toList();
        return export("Collections", List.of("Receipt", "Student", "Amount", "Method", "Paid at"), table, format);
    }

    public byte[] meritList(Long schoolId, Long examId, Role role, String format) {
        List<MeritRowResponse> rows = gradeService.meritList(schoolId, examId, role);
        List<List<String>> table = rows.stream()
                .map(row -> List.of(String.valueOf(row.position()), blank(row.admissionNo()), blank(row.studentName()),
                        money(row.total()), money(row.percentage()), blank(row.grade())))
                .toList();
        return export("Merit list", List.of("Position", "Admission", "Student", "Total", "Percent", "Grade"), table, format);
    }

    public byte[] attendanceSummary(Long schoolId, LocalDate start, LocalDate end, String format) {
        List<AttendanceSummaryResponse> rows = attendanceService.schoolSummaries(schoolId, start, end);
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
}
