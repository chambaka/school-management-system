package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.BulkGradeRequest;
import tz.co.chambaka.school.management.dto.academic.GradeGridResponse;
import tz.co.chambaka.school.management.dto.academic.GradeImportResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.export.XlsxDocuments;
import tz.co.chambaka.school.management.export.XlsxSheets;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeImportServiceTest {

    @Mock
    private GradeService gradeService;
    @Mock
    private ExamService examService;
    @InjectMocks
    private GradeImportService service;

    @Test
    void templateListsTheClassAndMarksAlreadyEntered() {
        when(gradeService.grid(1L, 2L, 3L)).thenReturn(grid());
        List<XlsxSheets.SheetRow> rows = XlsxSheets.rows(service.template(1L, 2L, 3L));
        assertThat(rows.get(0).cells()).containsExactly("Admission number", "Student name", "Marks");
        assertThat(rows.get(1).cells()).containsExactly("ADM-001", "Amina", "78");
        assertThat(rows.get(2).cells()).containsExactly("ADM-002", "Baraka", "");
    }

    @Test
    void importSavesFilledMarksAndSkipsBlankCells() {
        when(examService.require(1L, 2L)).thenReturn(Fixtures.exam());
        when(gradeService.grid(1L, 2L, 3L)).thenReturn(grid());
        byte[] file = XlsxDocuments.xlsx("Marks", List.of("Admission number", "Student name", "Marks"), List.of(
                List.of("ADM-001", "Amina", "80"),
                List.of("adm-002", "Baraka", "")
        ));
        GradeImportResponse result = service.importMarks(1L, 2L, 3L, file, 9L);
        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.problems()).isEmpty();
        ArgumentCaptor<BulkGradeRequest> captor = ArgumentCaptor.forClass(BulkGradeRequest.class);
        verify(gradeService).recordBulk(eq(1L), captor.capture(), eq(9L));
        assertThat(captor.getValue().entries()).hasSize(1);
        assertThat(captor.getValue().entries().getFirst().studentId()).isEqualTo(11L);
        assertThat(captor.getValue().entries().getFirst().marksObtained()).isEqualByComparingTo("80");
    }

    @Test
    void importDoesNotSaveWhenARowIsOutsideTheClass() {
        when(examService.require(1L, 2L)).thenReturn(Fixtures.exam());
        when(gradeService.grid(1L, 2L, 3L)).thenReturn(grid());
        byte[] file = XlsxDocuments.xlsx("Marks", List.of("Admission no", "Marks"), List.of(
                List.of("ZZZ", "10")
        ));
        GradeImportResponse result = service.importMarks(1L, 2L, 3L, file, 9L);
        assertThat(result.saved()).isZero();
        assertThat(result.problems().getFirst()).contains("not in this class");
        verify(gradeService, never()).recordBulk(any(), any(), any());
    }

    @Test
    void importRejectsASheetWithoutTheRequiredColumns() {
        when(examService.require(1L, 2L)).thenReturn(Fixtures.exam());
        when(gradeService.grid(1L, 2L, 3L)).thenReturn(grid());
        byte[] file = XlsxDocuments.xlsx("Marks", List.of("Name"), List.of(List.of("Amina")));
        assertThatThrownBy(() -> service.importMarks(1L, 2L, 3L, file, 9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Admission number");
    }

    private static GradeGridResponse grid() {
        return new GradeGridResponse(2L, "Midterm", 3L, "Mathematics", new BigDecimal("100"), new BigDecimal("40"),
                BigDecimal.ZERO, List.of(
                new GradeGridResponse.Row(11L, "Amina", "ADM-001", new BigDecimal("78"), "B", true, 1),
                new GradeGridResponse.Row(12L, "Baraka", "ADM-002", null, "", false, null)
        ));
    }
}
