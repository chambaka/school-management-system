package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.GradeRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.academic.ResultMath;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.Grade;
import tz.co.chambaka.school.management.model.ResultWeightConfig;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.enums.AssessmentComponent;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.ExamSubjectRepository;
import tz.co.chambaka.school.management.repository.GradeRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
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
    @Mock
    private ResultConfigService resultConfigService;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private GradeService service;

    @BeforeEach
    void resultLetters() {
        org.mockito.Mockito.lenient().when(resultConfigService.letterFor(any(), any()))
                .thenAnswer(invocation -> ResultMath.letter(invocation.getArgument(1), null));
    }

    @Test
    void recordAndListAndReportCardLetters() {
        when(examService.require(1L, 1L)).thenReturn(Fixtures.exam());
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(examSubjectRepository.findByExamIdAndSubjectId(1L, 1L)).thenReturn(Optional.of(Fixtures.examSubject()));
        when(gradeRepository.findByExamIdAndStudentIdAndSubjectId(1L, 1L, 1L)).thenReturn(Optional.empty());
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
        assertThat(service.reportCard(1L, 1L, 1L).termResults()).isEmpty();

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
        Grade existing = grade(new BigDecimal("40"));
        when(gradeRepository.findByExamIdAndStudentIdAndSubjectId(1L, 1L, 1L)).thenReturn(Optional.of(existing));
        when(teacherService.requireByUserSafe(1L)).thenReturn(Fixtures.teacher());
        when(gradeRepository.save(existing)).thenReturn(existing);
        assertThat(service.record(1L, new GradeRequest(1L, 1L, 1L, new BigDecimal("10"), null), 1L).marksObtained())
                .isEqualByComparingTo("10");
    }

    @Test
    void deleteRemovesSavedMarks() {
        when(examService.require(1L, 1L)).thenReturn(Fixtures.exam());
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        Grade existing = grade(new BigDecimal("40"));
        when(gradeRepository.findByExamIdAndStudentIdAndSubjectId(1L, 1L, 1L)).thenReturn(Optional.of(existing));
        service.delete(1L, 1L, 1L, 1L);
        org.mockito.Mockito.verify(gradeRepository).delete(existing);
        when(gradeRepository.findByExamIdAndStudentIdAndSubjectId(1L, 1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(1L, 1L, 1L, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void gridRanksActiveStudentsAndComputesAverage() {
        Exam exam = Fixtures.exam();
        when(examService.require(1L, 1L)).thenReturn(exam);
        when(examSubjectRepository.findByExamIdAndSubjectId(1L, 1L)).thenReturn(Optional.of(Fixtures.examSubject()));
        Student first = Fixtures.student();
        first.getUser().setName("Alice");
        Student second = Fixtures.student();
        second.setId(2L);
        second.getUser().setName("Bob");
        second.setStatus(StudentStatus.ACTIVE);
        Student archived = Fixtures.student();
        archived.setId(3L);
        archived.setStatus(StudentStatus.ARCHIVED);
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of(second, archived, first));
        Grade grade = grade(new BigDecimal("80"));
        grade.setStudent(first);
        when(gradeRepository.findByExamIdAndSubjectId(1L, 1L)).thenReturn(List.of(grade));

        var grid = service.grid(1L, 1L, 1L);

        assertThat(grid.subjectAverage()).isEqualByComparingTo("80");
        assertThat(grid.rows()).hasSize(2);
        assertThat(grid.rows().getFirst().classPosition()).isEqualTo(1);
        assertThat(grid.rows().get(1).marksObtained()).isNull();
    }

    @Test
    void bulkRecordsEveryEntryAndTermResultUsesConfiguredWeights() {
        when(examService.require(1L, 1L)).thenReturn(Fixtures.exam());
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(examSubjectRepository.findByExamIdAndSubjectId(1L, 1L)).thenReturn(Optional.of(Fixtures.examSubject()));
        when(gradeRepository.findByExamIdAndStudentIdAndSubjectId(1L, 1L, 1L)).thenReturn(Optional.empty());
        when(teacherService.requireByUserSafe(3L)).thenReturn(Fixtures.teacher());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        var bulk = new tz.co.chambaka.school.management.dto.academic.BulkGradeRequest(
                1L, 1L, List.of(
                new tz.co.chambaka.school.management.dto.academic.BulkGradeRequest.Entry(
                        1L, BigDecimal.valueOf(70), "Good")));
        assertThat(service.recordBulk(1L, bulk, 3L)).hasSize(1);

        Exam midterm = componentExam(1L, AssessmentComponent.MIDTERM);
        tz.co.chambaka.school.management.model.AcademicTerm term = new tz.co.chambaka.school.management.model.AcademicTerm();
        term.setId(1L);
        term.setName("Term 1");
        midterm.setAcademicTerm(term);
        Exam semi = componentExam(2L, AssessmentComponent.SEMI_TERMINAL);
        Exam terminal = componentExam(3L, AssessmentComponent.TERMINAL);
        Exam otherClass = componentExam(9L, AssessmentComponent.MIDTERM);
        tz.co.chambaka.school.management.model.SchoolClass other = Fixtures.schoolClass();
        other.setId(99L);
        otherClass.setSchoolClass(other);
        when(examRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateDesc(1L, 1L))
                .thenReturn(List.of(midterm, semi, terminal, otherClass));
        when(gradeRepository.findByExamIdAndStudentId(1L, 1L)).thenReturn(List.of(componentGrade(midterm, "80")));
        when(gradeRepository.findByExamIdAndStudentId(2L, 1L)).thenReturn(List.of(componentGrade(semi, "70")));
        when(gradeRepository.findByExamIdAndStudentId(3L, 1L)).thenReturn(List.of(componentGrade(terminal, "90")));
        ResultWeightConfig weights = new ResultWeightConfig();
        weights.setMidtermWeight(BigDecimal.TEN);
        weights.setSemiExamWeight(BigDecimal.valueOf(90));
        weights.setSemiResultWeight(BigDecimal.valueOf(50));
        weights.setTerminalExamWeight(BigDecimal.valueOf(50));
        when(resultConfigService.resolve(1L, 1L, 1L, 1L)).thenReturn(weights);

        var result = service.termResult(1L, 1L, 1L, 1L, 1L);
        assertThat(result.midterm()).isEqualByComparingTo("80");
        assertThat(result.subjectName()).isEqualTo("Mathematics");
        assertThat(result.admissionNo()).isEqualTo(Fixtures.student().getAdmissionNo());
        assertThat(result.terminalResult()).isEqualByComparingTo("80.50");

        when(examSubjectRepository.findByExamIdIn(any())).thenReturn(List.of(Fixtures.examSubject()));
        var report = service.termReport(1L, 1L, 1L, 1L, tz.co.chambaka.school.management.model.enums.Role.HEADMASTER);
        assertThat(report.subjects()).hasSize(1);
        assertThat(report.average()).isEqualByComparingTo("80.50");
        assertThat(report.academicYearName()).isEqualTo("2026/2027");
        assertThat(report.academicTermName()).isEqualTo("Term 1");

        Student archived = Fixtures.student();
        archived.setId(8L);
        archived.setStatus(StudentStatus.ARCHIVED);
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of(Fixtures.student(), archived));
        assertThat(service.classTermResults(1L, 1L, 1L, 1L, 1L, tz.co.chambaka.school.management.model.enums.Role.TEACHER))
                .hasSize(1);

        when(examRepository.findBySchoolIdAndAcademicYearIdAndPublishedTrueOrderByStartDateDesc(1L, 1L))
                .thenReturn(List.of());
        assertThat(service.termReport(1L, 1L, 1L, 1L, tz.co.chambaka.school.management.model.enums.Role.STUDENT).subjects())
                .isEmpty();
        assertThat(service.termResult(1L, 1L, 1L, 1L, 1L, tz.co.chambaka.school.management.model.enums.Role.PARENT).subjectName())
                .isEqualTo("Subject");
        assertThat(service.reportCard(1L, 1L, 1L).termResults()).isNotEmpty();

        Exam noYear = Fixtures.exam();
        noYear.setId(5L);
        noYear.setAcademicYear(null);
        when(examService.require(1L, 5L)).thenReturn(noYear);
        when(gradeRepository.findByExamIdAndStudentId(5L, 1L)).thenReturn(List.of());
        assertThat(service.reportCard(1L, 1L, 5L).termResults()).isEmpty();

        Student noClass = Fixtures.student();
        noClass.setId(4L);
        noClass.setSchoolClass(null);
        when(studentService.require(1L, 4L)).thenReturn(noClass);
        assertThat(service.termReport(1L, 4L, 1L, null, tz.co.chambaka.school.management.model.enums.Role.HEADMASTER).className())
                .isNull();
        assertThat(service.classTermResults(1L, 1L, null, 1L, 99L, tz.co.chambaka.school.management.model.enums.Role.TEACHER)
                .getFirst().subjectName()).isEqualTo("Subject");
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

    private Exam componentExam(Long id, AssessmentComponent component) {
        Exam exam = Fixtures.exam();
        exam.setId(id);
        exam.setAssessmentComponent(component);
        return exam;
    }

    private Grade componentGrade(Exam exam, String marks) {
        Grade grade = grade(new BigDecimal(marks));
        grade.setExam(exam);
        return grade;
    }
}
