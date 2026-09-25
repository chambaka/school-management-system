package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.ExamRequest;
import tz.co.chambaka.school.management.dto.academic.ExamSubjectRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Exam;
import tz.co.chambaka.school.management.model.ExamSeat;
import tz.co.chambaka.school.management.model.ExamSubject;
import tz.co.chambaka.school.management.model.TeacherSubject;
import tz.co.chambaka.school.management.model.enums.ExamApprovalStatus;
import tz.co.chambaka.school.management.model.enums.ExamType;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.ClassroomRepository;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.ExamSeatRepository;
import tz.co.chambaka.school.management.repository.ExamSubjectRepository;
import tz.co.chambaka.school.management.repository.GradeRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
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
import static org.mockito.Mockito.verify;
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
    @Mock
    private TeacherService teacherService;
    @Mock
    private AcademicTermService academicTermService;
    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private ExamSeatRepository examSeatRepository;
    @Mock
    private AlertService alertService;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;
    @InjectMocks
    private ExamService service;

    @Test
    void listShowsYearTermSubjectsTeachersAndClass() {
        Exam exam = Fixtures.exam();
        exam.setAcademicTerm(new tz.co.chambaka.school.management.model.AcademicTerm());
        exam.getAcademicTerm().setId(1L);
        exam.getAcademicTerm().setName("Term 1");
        ExamSubject paper = Fixtures.examSubject();
        TeacherSubject allocation = new TeacherSubject();
        allocation.setAcademicYear(exam.getAcademicYear());
        allocation.setSchoolClass(exam.getSchoolClass());
        allocation.setSubject(Fixtures.subject());
        allocation.setTeacher(Fixtures.teacher());
        when(examRepository.findBySchoolIdOrderByStartDateDesc(1L)).thenReturn(List.of(exam));
        when(examSubjectRepository.findByExamIdIn(List.of(1L))).thenReturn(List.of(paper));
        when(teacherSubjectRepository.findBySchoolId(1L)).thenReturn(List.of(allocation));

        var row = service.list(1L, null).getFirst();
        assertThat(row.academicYearName()).isEqualTo(exam.getAcademicYear().getName());
        assertThat(row.academicTermName()).isEqualTo("Term 1");
        assertThat(row.schoolClassName()).isEqualTo(exam.getSchoolClass().getName());
        assertThat(row.subjects()).contains("Mathematics");
        assertThat(row.teachers()).contains(Fixtures.teacher().getUser().getName());
        assertThat(row.startDate()).isEqualTo(exam.getStartDate());
        assertThat(row.endDate()).isEqualTo(exam.getEndDate());
    }

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
        exam.setApprovalStatus(ExamApprovalStatus.APPROVED);
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of());
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

    @Test
    void approvalChainAndVisibilityRules() {
        Exam exam = Fixtures.exam();
        exam.setApprovalStatus(ExamApprovalStatus.DRAFT);
        when(examRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(exam));
        assertThat(service.submit(1L, 1L).approvalStatus()).isEqualTo(ExamApprovalStatus.ENTERED);
        assertThat(service.verify(1L, 1L).approvalStatus()).isEqualTo(ExamApprovalStatus.VERIFIED);
        assertThat(service.approve(1L, 1L).approvalStatus()).isEqualTo(ExamApprovalStatus.APPROVED);
        assertThat(exam.getVerifiedAt()).isNotNull();
        assertThat(exam.getApprovedAt()).isNotNull();

        exam.setApprovalStatus(ExamApprovalStatus.DRAFT);
        assertThatThrownBy(() -> service.verify(1L, 1L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.approve(1L, 1L)).isInstanceOf(BusinessException.class);
        exam.setApprovalStatus(ExamApprovalStatus.VERIFIED);
        assertThat(service.reject(1L, 1L, "Fix totals").approvalStatus()).isEqualTo(ExamApprovalStatus.REJECTED);
        assertThat(exam.getRejectionNote()).isEqualTo("Fix totals");
        assertThat(service.submit(1L, 1L).approvalStatus()).isEqualTo(ExamApprovalStatus.ENTERED);
        exam.setApprovalStatus(ExamApprovalStatus.APPROVED);
        assertThatThrownBy(() -> service.reject(1L, 1L, "no")).isInstanceOf(BusinessException.class);
        exam.setApprovalStatus(ExamApprovalStatus.VERIFIED);
        assertThatThrownBy(() -> service.submit(1L, 1L)).isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> service.publish(1L, 1L, true)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.assertMarksEditable(exam)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.assertVisibleTo(exam, Role.PARENT))
                .isInstanceOf(ResourceNotFoundException.class);
        service.assertVisibleTo(exam, Role.HEADMASTER);
    }

    @Test
    void parentsAndStudentsListOnlyPublishedExams() {
        Exam exam = Fixtures.exam();
        exam.setPublished(true);
        when(examRepository.findBySchoolIdAndPublishedTrueOrderByStartDateDesc(1L)).thenReturn(List.of(exam));
        when(examRepository.findBySchoolIdAndAcademicYearIdAndPublishedTrueOrderByStartDateDesc(1L, 1L))
                .thenReturn(List.of(exam));
        assertThat(service.list(1L, null, Role.PARENT)).hasSize(1);
        assertThat(service.list(1L, 1L, Role.STUDENT)).hasSize(1);
    }

    @Test
    void generatesLocksAndReadsSeats() {
        Exam exam = Fixtures.exam();
        exam.setStartDate(LocalDate.of(2026, 3, 6));
        exam.setEndDate(LocalDate.of(2026, 3, 10));
        ExamSubject first = Fixtures.examSubject();
        ExamSubject second = Fixtures.examSubject();
        second.setId(2L);
        when(examRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(exam));
        when(examSubjectRepository.findByExamId(1L)).thenReturn(List.of(first, second));
        when(classroomRepository.findBySchoolIdOrderByNameAsc(1L)).thenReturn(List.of(Fixtures.classroom()));
        when(teacherRepository.findAllBySchoolId(1L)).thenReturn(List.of(Fixtures.teacher()));
        var active = Fixtures.student();
        active.setStatus(StudentStatus.ACTIVE);
        var archived = Fixtures.student();
        archived.setId(2L);
        archived.setStatus(StudentStatus.ARCHIVED);
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of(active, archived));
        when(examSeatRepository.save(any(ExamSeat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var schedule = service.generateSchedule(1L, 1L);
        assertThat(schedule).hasSize(2);
        assertThat(schedule.getFirst().startTime()).isEqualTo(java.time.LocalTime.of(8, 0));
        assertThat(schedule.get(1).startTime()).isEqualTo(java.time.LocalTime.of(11, 0));
        assertThat(service.lockSchedule(1L, 1L, true).scheduleLocked()).isTrue();
        assertThatThrownBy(() -> service.generateSchedule(1L, 1L)).isInstanceOf(BusinessException.class);

        ExamSeat seat = new ExamSeat();
        seat.setId(1L);
        seat.setExamSubject(first);
        seat.setStudent(active);
        seat.setSeatNumber("S-01");
        when(examSeatRepository.findByExamSubjectIdOrderBySeatNumberAsc(1L)).thenReturn(List.of(seat));
        assertThat(service.seats(1L, 1L)).singleElement()
                .satisfies(row -> assertThat(row.admissionNo()).isEqualTo("ADM-001"));
    }

    @Test
    void unpublishingRestoresApprovedStatusAndLockedExamRejectsSubject() {
        Exam exam = Fixtures.exam();
        exam.setApprovalStatus(ExamApprovalStatus.PUBLISHED);
        exam.setPublished(true);
        when(examRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(exam));
        assertThat(service.publish(1L, 1L, false).approvalStatus()).isEqualTo(ExamApprovalStatus.APPROVED);

        exam.setScheduleLocked(true);
        assertThatThrownBy(() -> service.addSubject(1L, 1L,
                new ExamSubjectRequest(1L, BigDecimal.TEN, BigDecimal.ONE, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deleteRemovesUnusedExamAndItsPapers() {
        Exam exam = Fixtures.exam();
        ExamSubject paper = Fixtures.examSubject();
        when(examRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(exam));
        when(gradeRepository.existsByExamId(1L)).thenReturn(false);
        when(examSubjectRepository.findByExamId(1L)).thenReturn(List.of(paper));

        service.delete(1L, 1L);

        verify(examSeatRepository).deleteByExamSubjectId(paper.getId());
        verify(examSubjectRepository).deleteByExamId(1L);
        verify(examRepository).delete(exam);
    }

    @Test
    void deleteBlockedWhenPublishedOrHasMarks() {
        Exam exam = Fixtures.exam();
        exam.setPublished(true);
        when(examRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(exam));
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unpublish");

        exam.setPublished(false);
        when(gradeRepository.existsByExamId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("marks");
    }

    @Test
    void updateAndDeletePaper() {
        Exam exam = Fixtures.exam();
        ExamSubject paper = Fixtures.examSubject();
        when(examRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(exam));
        when(examSubjectRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(paper));
        when(examSubjectRepository.save(paper)).thenReturn(paper);
        when(gradeRepository.existsByExamSubjectId(1L)).thenReturn(false);
        when(teacherService.require(1L, 1L)).thenReturn(Fixtures.teacher());
        ExamSubjectRequest update = new ExamSubjectRequest(
                1L, new BigDecimal("80"), new BigDecimal("30"), LocalDate.of(2026, 3, 3),
                java.time.LocalTime.of(8, 0), java.time.LocalTime.of(10, 0), "Hall A", 1L);
        assertThat(service.updateSubject(1L, 1L, 1L, update).maxMarks()).isEqualByComparingTo("80");
        assertThat(service.updateSubject(1L, 1L, 1L, new ExamSubjectRequest(
                1L, new BigDecimal("80"), new BigDecimal("30"), null)).invigilatorId()).isNull();

        tz.co.chambaka.school.management.model.Subject english = Fixtures.subject();
        english.setId(2L);
        english.setName("English");
        when(subjectService.require(1L, 2L)).thenReturn(english);
        when(examSubjectRepository.existsByExamIdAndSubjectId(1L, 2L)).thenReturn(false);
        ExamSubjectRequest renamed = new ExamSubjectRequest(2L, new BigDecimal("80"), new BigDecimal("30"), null);
        assertThat(service.updateSubject(1L, 1L, 1L, renamed).subjectName()).isEqualTo("English");

        service.deleteSubject(1L, 1L, 1L);
        verify(examSeatRepository).deleteByExamSubjectId(1L);
        verify(examSubjectRepository).delete(paper);
    }

    @Test
    void updateAndDeletePaperErrors() {
        Exam exam = Fixtures.exam();
        ExamSubject paper = Fixtures.examSubject();
        when(examRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(exam));
        when(examSubjectRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(paper));
        ExamSubjectRequest req = new ExamSubjectRequest(1L, new BigDecimal("40"), new BigDecimal("50"), null);
        assertThatThrownBy(() -> service.updateSubject(1L, 1L, 1L, req)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pass marks");

        ExamSubjectRequest ok = new ExamSubjectRequest(2L, new BigDecimal("80"), new BigDecimal("30"), null);
        when(gradeRepository.existsByExamSubjectId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.updateSubject(1L, 1L, 1L, ok)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("marks");
        assertThatThrownBy(() -> service.deleteSubject(1L, 1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("marks");

        when(gradeRepository.existsByExamSubjectId(1L)).thenReturn(false);
        when(examSubjectRepository.existsByExamIdAndSubjectId(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.updateSubject(1L, 1L, 1L, ok))
                .isInstanceOf(DuplicateResourceException.class);

        exam.setScheduleLocked(true);
        assertThatThrownBy(() -> service.updateSubject(1L, 1L, 1L, ok)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("locked");

        exam.setScheduleLocked(false);
        exam.setPublished(true);
        assertThatThrownBy(() -> service.deleteSubject(1L, 1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unpublish");

        paper.getExam().setId(9L);
        assertThatThrownBy(() -> service.updateSubject(1L, 1L, 1L, ok)).isInstanceOf(ResourceNotFoundException.class);
        when(examSubjectRepository.findByIdAndSchoolId(8L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteSubject(1L, 1L, 8L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
