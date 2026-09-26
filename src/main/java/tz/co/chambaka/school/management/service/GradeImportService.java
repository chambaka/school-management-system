package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.BulkGradeRequest;
import tz.co.chambaka.school.management.dto.academic.GradeGridResponse;
import tz.co.chambaka.school.management.dto.academic.GradeImportResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.export.XlsxDocuments;
import tz.co.chambaka.school.management.export.XlsxSheets;
import tz.co.chambaka.school.management.model.Exam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GradeImportService {

    private static final List<String> ADMISSION_HEADERS = List.of(
            "admission number", "admission no", "admission no.", "adm", "admission");
    private static final List<String> MARK_HEADERS = List.of("marks", "marks obtained", "mark", "score");

    private final GradeService gradeService;
    private final ExamService examService;

    public GradeImportService(GradeService gradeService, ExamService examService) {
        this.gradeService = gradeService;
        this.examService = examService;
    }

    @Transactional(readOnly = true)
    public byte[] template(Long schoolId, Long examId, Long subjectId) {
        GradeGridResponse grid = gradeService.grid(schoolId, examId, subjectId);
        List<GradeGridResponse.Row> students = grid.rows().stream()
                .sorted(Comparator.comparing(GradeGridResponse.Row::studentName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<List<String>> rows = new ArrayList<>();
        for (GradeGridResponse.Row row : students) {
            String marks = row.marksObtained() == null
                    ? ""
                    : row.marksObtained().stripTrailingZeros().toPlainString();
            rows.add(List.of(
                    row.admissionNo() == null ? "" : row.admissionNo(),
                    row.studentName() == null ? "" : row.studentName(),
                    marks
            ));
        }
        return XlsxDocuments.xlsx("Marks", List.of("Admission number", "Student name", "Marks"), rows);
    }

    @Transactional
    public GradeImportResponse importMarks(Long schoolId, Long examId, Long subjectId, byte[] file, Long userId) {
        if (file == null || file.length == 0) {
            throw new BusinessException("Choose an Excel file.");
        }
        Exam exam = examService.require(schoolId, examId);
        examService.assertMarksEditable(exam);
        GradeGridResponse grid = gradeService.grid(schoolId, examId, subjectId);
        List<XlsxSheets.SheetRow> rows;
        try {
            rows = XlsxSheets.rows(file);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ex.getMessage());
        }
        if (rows.isEmpty()) {
            throw new BusinessException("The spreadsheet is empty.");
        }
        int headerAt = headerRow(rows);
        if (headerAt < 0) {
            throw new BusinessException("The spreadsheet needs columns Admission number and Marks. Download the template from Grades.");
        }
        XlsxSheets.SheetRow header = rows.get(headerAt);
        int admissionCol = column(header.cells(), ADMISSION_HEADERS);
        int marksCol = column(header.cells(), MARK_HEADERS);
        Map<String, GradeGridResponse.Row> byAdmission = new LinkedHashMap<>();
        for (GradeGridResponse.Row row : grid.rows()) {
            if (row.admissionNo() != null && !row.admissionNo().isBlank()) {
                byAdmission.put(normalize(row.admissionNo()), row);
            }
        }
        List<String> problems = new ArrayList<>();
        List<BulkGradeRequest.Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int skipped = 0;
        for (int i = headerAt + 1; i < rows.size(); i++) {
            XlsxSheets.SheetRow row = rows.get(i);
            String admission = cell(row.cells(), admissionCol).trim();
            String marksText = cell(row.cells(), marksCol).trim();
            if (admission.isBlank() && marksText.isBlank()) {
                continue;
            }
            if (marksText.isBlank()) {
                skipped++;
                continue;
            }
            if (admission.isBlank()) {
                problems.add("Row " + row.number() + ": enter the admission number.");
                continue;
            }
            String key = normalize(admission);
            if (!seen.add(key)) {
                problems.add("Row " + row.number() + ": admission number " + admission + " is listed more than once.");
                continue;
            }
            GradeGridResponse.Row student = byAdmission.get(key);
            if (student == null) {
                problems.add("Row " + row.number() + ": admission number " + admission + " is not in this class for this exam.");
                continue;
            }
            BigDecimal marks;
            try {
                marks = parseMarks(marksText);
            } catch (NumberFormatException ex) {
                problems.add("Row " + row.number() + ": marks must be a number.");
                continue;
            }
            if (marks.compareTo(BigDecimal.ZERO) < 0 || marks.compareTo(grid.maxMarks()) > 0) {
                problems.add("Row " + row.number() + ": marks must be between 0 and "
                        + grid.maxMarks().stripTrailingZeros().toPlainString() + ".");
                continue;
            }
            entries.add(new BulkGradeRequest.Entry(student.studentId(), marks, null));
        }
        if (!problems.isEmpty()) {
            return new GradeImportResponse(0, skipped, problems);
        }
        if (entries.isEmpty()) {
            throw new BusinessException("Enter at least one mark. Blank mark cells are left unchanged.");
        }
        gradeService.recordBulk(schoolId, new BulkGradeRequest(examId, subjectId, entries), userId);
        return new GradeImportResponse(entries.size(), skipped, List.of());
    }

    private static int headerRow(List<XlsxSheets.SheetRow> rows) {
        int limit = Math.min(rows.size(), 5);
        for (int i = 0; i < limit; i++) {
            if (column(rows.get(i).cells(), ADMISSION_HEADERS) >= 0 && column(rows.get(i).cells(), MARK_HEADERS) >= 0) {
                return i;
            }
        }
        return -1;
    }

    private static int column(List<String> cells, List<String> names) {
        for (int i = 0; i < cells.size(); i++) {
            if (names.contains(normalize(cells.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    private static String cell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size() || cells.get(index) == null) {
            return "";
        }
        return cells.get(index);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static BigDecimal parseMarks(String raw) {
        String text = raw.replace(" ", "");
        if (text.indexOf(',') >= 0 && text.indexOf('.') < 0) {
            text = text.replace(',', '.');
        }
        return new BigDecimal(text);
    }
}
