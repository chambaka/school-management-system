package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.dto.academic.GradeResponse;
import tz.co.chambaka.school.management.dto.academic.ReportCardResponse;
import tz.co.chambaka.school.management.model.enums.Role;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock GradeService gradeService;
    @Mock AttendanceService attendanceService;
    @Mock FinanceService financeService;
    @Mock StudentService studentService;
    @InjectMocks ReportExportService service;

    @Test
    void exportsReportCardAsPdfAndCsv() {
        when(gradeService.reportCard(1L, 1L, 1L, Role.PARENT)).thenReturn(card(2));

        byte[] pdf = service.reportCardPdf(1L, 1L, 1L, Role.PARENT);
        byte[] csv = service.reportCardCsv(1L, 1L, 1L, Role.PARENT);

        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).startsWith("%PDF-1.4");
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains("Subject,Marks,Max,Passed")
                .contains("Mathematics,78,100,Yes");
    }

    @Test
    void pdfHandlesMissingSectionAndPosition() {
        when(gradeService.reportCard(1L, 1L, 1L, Role.STUDENT)).thenReturn(card(null));
        assertThat(service.reportCardPdf(1L, 1L, 1L, Role.STUDENT)).isNotEmpty();
    }

    private ReportCardResponse card(Integer position) {
        GradeResponse subject = new GradeResponse(
                1L, 1L, 1L, "Student", 1L, "Mathematics",
                BigDecimal.valueOf(78), BigDecimal.valueOf(100), BigDecimal.valueOf(40), true, "Good");
        return new ReportCardResponse(
                1L, "Student", "ADM-001", "Form 1", position == null ? null : "A",
                1L, "Midterm", List.of(subject), BigDecimal.valueOf(78), BigDecimal.valueOf(100),
                BigDecimal.valueOf(78), "B", BigDecimal.valueOf(4), position, true);
    }
}
