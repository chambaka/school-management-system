package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.ReportCardResponse;
import tz.co.chambaka.school.management.export.SimpleDocuments;
import tz.co.chambaka.school.management.model.enums.Role;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportExportService {

    private final GradeService gradeService;
    private final AttendanceService attendanceService;
    private final FinanceService financeService;
    private final StudentService studentService;

    public ReportExportService(
            GradeService gradeService,
            AttendanceService attendanceService,
            FinanceService financeService,
            StudentService studentService
    ) {
        this.gradeService = gradeService;
        this.attendanceService = attendanceService;
        this.financeService = financeService;
        this.studentService = studentService;
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
}
