package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.GradeRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Grade;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.repository.ExamSubjectRepository;
import tz.co.chambaka.school.management.repository.GradeRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private ExamSubjectRepository examSubjectRepository;
    @Mock
    private ExamService examService;
    @Mock
    private StudentService studentService;
    @Mock
    private TeacherService teacherService;
    @InjectMocks
    private GradeService service;

    @Test
    void recordAndListAndReportCardLetters() {
        when(examService.require(1L, 1L)).thenReturn(Fixtures.exam());
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(examSubjectRepository.findByExamIdAndSubjectId(1L, 1L)).thenReturn(Optional.of(Fixtures.examSubject()));
        when(gradeRepository.existsByExamIdAndStudentIdAndSubjectId(1L, 1L, 1L)).thenReturn(false);
        when(teacherService.requireByUserSafe(3L)).thenReturn(Fixtures.teacher());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> {
            Grade saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        GradeRequest req = new GradeRequest(1L, 1L, 1L, new BigDecimal("78"), "Good");
        assertThat(service.record(1L, req, 3L).passed()).isTrue();

        Grade grade = grade(new BigDecimal("78"));
        when(gradeRepository.findByExamIdAndSchoolId(1L, 1L)).thenReturn(List.of(grade));
        assertThat(service.byExam(1L, 1L)).hasSize(1);

        when(gradeRepository.findByExamIdAndStudentId(1L, 1L)).thenReturn(List.of(grade));
        assertThat(service.reportCard(1L, 1L, 1L).overallGrade()).isEqualTo("B");

        Student bare = Fixtures.student();
        bare.setSchoolClass(null);
        bare.setSection(null);
        when(studentService.require(1L, 2L)).thenReturn(bare);
        when(gradeRepository.findByExamIdAndStudentId(1L, 2L)).thenReturn(List.of());
        assertThat(service.reportCard(1L, 2L, 1L).percentage()).isEqualByComparingTo("0");
        assertThat(service.reportCard(1L, 2L, 1L).overallGrade()).isEqualTo("F");
    }

    @Test
    void letterGradesViaDifferentMarks() {
        when(examService.require(1L, 1L)).thenReturn(Fixtures.exam());
        Student student = Fixtures.student();
        when(studentService.require(1L, 1L)).thenReturn(student);
        assertThat(card(new BigDecimal("85")).overallGrade()).isEqualTo("A");
        assertThat(card(new BigDecimal("70")).overallGrade()).isEqualTo("B");
        assertThat(card(new BigDecimal("60")).overallGrade()).isEqualTo("C");
        assertThat(card(new BigDecimal("50")).overallGrade()).isEqualTo("D");
        assertThat(card(new BigDecimal("40")).overallGrade()).isEqualTo("F");
    }

    @Test
    void recordErrors() {
        when(examService.require(1L, 1L)).thenReturn(Fixtures.exam());
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(examSubjectRepository.findByExamIdAndSubjectId(1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.record(1L, new GradeRequest(1L, 1L, 1L, BigDecimal.TEN, null), 1L))
                .isInstanceOf(ResourceNotFoundException.class);
        when(examSubjectRepository.findByExamIdAndSubjectId(1L, 1L)).thenReturn(Optional.of(Fixtures.examSubject()));
        assertThatThrownBy(() -> service.record(1L, new GradeRequest(1L, 1L, 1L, new BigDecimal("200"), null), 1L))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.record(1L, new GradeRequest(1L, 1L, 1L, new BigDecimal("-1"), null), 1L))
                .isInstanceOf(BusinessException.class);
        when(gradeRepository.existsByExamIdAndStudentIdAndSubjectId(1L, 1L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.record(1L, new GradeRequest(1L, 1L, 1L, new BigDecimal("10"), null), 1L))
                .isInstanceOf(DuplicateResourceException.class);
    }

    private tz.co.chambaka.school.management.dto.academic.ReportCardResponse card(BigDecimal marks) {
        when(gradeRepository.findByExamIdAndStudentId(1L, 1L)).thenReturn(List.of(grade(marks)));
        return service.reportCard(1L, 1L, 1L);
    }

    private Grade grade(BigDecimal marks) {
        Grade grade = new Grade();
        grade.setId(1L);
        grade.setExam(Fixtures.exam());
        grade.setExamSubject(Fixtures.examSubject());
        grade.setStudent(Fixtures.student());
        grade.setSubject(Fixtures.subject());
        grade.setMarksObtained(marks);
        return grade;
    }
}
