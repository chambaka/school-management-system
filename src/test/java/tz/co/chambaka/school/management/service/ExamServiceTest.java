package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.ExamRequest;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.ExamSubject;
import tz.co.chambaka.school.management.model.enums.ExamType;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.ExamSubjectRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamSubjectRepository examSubjectRepository;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private ClassService classService;
    @Mock
    private SubjectService subjectService;
    @InjectMocks
    private ExamService service;

    @Test
    void listCreatePublishAddSubject() {
        Exam exam = Fixtures.exam();
        when(examRepository.findBySchoolIdOrderByStartDateDesc(1L)).thenReturn(List.of(exam));
        when(examRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateDesc(1L, 1L)).thenReturn(List.of(exam));
        assertThat(service.list(1L, null)).hasSize(1);
        assertThat(service.list(1L, 1L).getFirst().name()).isEqualTo("Midterm");

        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> {
            Exam saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        ExamRequest req = new ExamRequest(1L, 1L, "Final", ExamType.FINAL,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));
        assertThat(service.create(1L, req).examType()).isEqualTo(ExamType.FINAL);

        when(examRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(exam));
        assertThat(service.publish(1L, 1L, true).published()).isTrue();

        when(examSubjectRepository.existsByExamIdAndSubjectId(1L, 1L)).thenReturn(false);
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(examSubjectRepository.save(any(ExamSubject.class))).thenAnswer(inv -> {
            ExamSubject saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        ExamSubjectRequest paper = new ExamSubjectRequest(1L, new BigDecimal("100"), new BigDecimal("40"),
                LocalDate.of(2026, 6, 2));
        assertThat(service.addSubject(1L, 1L, paper).subjectName()).isEqualTo("Mathematics");

        when(examSubjectRepository.findByExamId(1L)).thenReturn(List.of(Fixtures.examSubject()));
        assertThat(service.listSubjects(1L, 1L)).hasSize(1);
    }

    @Test
    void errors() {
        ExamRequest bad = new ExamRequest(1L, 1L, "X", ExamType.QUIZ,
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 1));
        assertThatThrownBy(() -> service.create(1L, bad)).isInstanceOf(BusinessException.class);
        when(examRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(Fixtures.exam()));
        when(examSubjectRepository.existsByExamIdAndSubjectId(1L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.addSubject(1L, 1L,
                new ExamSubjectRequest(1L, new BigDecimal("100"), new BigDecimal("40"), null)))
                .isInstanceOf(DuplicateResourceException.class);
        when(examSubjectRepository.existsByExamIdAndSubjectId(1L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> service.addSubject(1L, 1L,
                new ExamSubjectRequest(2L, new BigDecimal("40"), new BigDecimal("50"), null)))
                .isInstanceOf(BusinessException.class);
        when(examRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
