package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tz.co.chambaka.school.management.dto.academic.AssignmentRequest;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Assignment;
import tz.co.chambaka.school.management.model.AssignmentSubmission;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.AssignmentRepository;
import tz.co.chambaka.school.management.repository.AssignmentSubmissionRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.support.Fixtures;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock AssignmentRepository assignmentRepository;
    @Mock AssignmentSubmissionRepository submissionRepository;
    @Mock TeacherService teacherService;
    @Mock ClassService classService;
    @Mock SectionService sectionService;
    @Mock SubjectService subjectService;
    @Mock StudentService studentService;
    @Mock StudentRepository studentRepository;
    @Mock PhotoStorageService photoStorageService;
    @Mock AlertService alertService;
    @Mock AllocationService allocationService;
    @InjectMocks AssignmentService service;

    @Test
    void createsAssignmentAndAlertsClassParents() {
        when(teacherService.requireByUser(3L)).thenReturn(Fixtures.teacher());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of(Fixtures.student()));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var response = service.create(1L, 3L, new AssignmentRequest(
                1L, 1L, 1L, "Algebra", "Questions 1-5", LocalDate.of(2026, 9, 20)));

        assertThat(response.title()).isEqualTo("Algebra");
        assertThat(response.sectionName()).isEqualTo("A");
        verify(alertService).notifyParentsOfStudent(any(), any(), any(), any(), any(), any(Boolean.class));
    }

    @Test
    void createsWithoutSectionAndListsByRole() {
        when(teacherService.requireByUser(3L)).thenReturn(Fixtures.teacher());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(studentRepository.findBySchoolIdAndSchoolClassId(1L, 1L)).thenReturn(List.of());
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        assertThat(service.create(1L, 3L, new AssignmentRequest(
                1L, null, 1L, "General", null, LocalDate.now().plusDays(2))).sectionId()).isNull();

        Assignment matching = assignment(1L, Fixtures.schoolClass());
        var otherClass = Fixtures.schoolClass();
        otherClass.setId(2L);
        Assignment other = assignment(2L, otherClass);
        when(assignmentRepository.findBySchoolIdOrderByDueDateDesc(1L)).thenReturn(List.of(matching, other));
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        assertThat(service.list(1L, Fixtures.principal(Role.STUDENT))).hasSize(1);
        assertThat(service.list(1L, Fixtures.principal(Role.HEADMASTER))).hasSize(2);

        Student withoutClass = Fixtures.student();
        withoutClass.setSchoolClass(null);
        when(studentService.requireByUser(10L)).thenReturn(withoutClass);
        assertThat(service.list(1L, Fixtures.principal(Role.STUDENT))).isEmpty();
    }

    @Test
    void attachesFileAndRejectsMissingAssignment() {
        Assignment assignment = assignment(1L, Fixtures.schoolClass());
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(assignment));
        MockMultipartFile file = new MockMultipartFile("file", "work.pdf", "application/pdf", new byte[]{1});
        assertThat(service.attach(1L, 3L, 1L, file).attachmentName()).isEqualTo("work.pdf");
        verify(photoStorageService).storeStudentPhoto(1L, 900001L, file);

        when(assignmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.attach(1L, 3L, 9L, file)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void studentSubmitsWorkAndListsSubmissions() {
        Assignment assignment = assignment(1L, Fixtures.schoolClass());
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(assignment));
        when(studentService.requireByUser(10L)).thenReturn(Fixtures.student());
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> {
            AssignmentSubmission saved = inv.getArgument(0);
            saved.setId(3L);
            return saved;
        });
        MockMultipartFile file = new MockMultipartFile("file", "essay.pdf", "application/pdf", new byte[]{2});
        var submitted = service.submitWork(1L, 10L, 1L, "ready", file);
        assertThat(submitted.attachmentName()).isEqualTo("essay.pdf");
        verify(alertService).notifyParentsOfStudent(eq(1L), any(Student.class), eq("Assignment submitted"), any(), eq("ASSIGNMENT"), eq(false));

        AssignmentSubmission existing = new AssignmentSubmission();
        existing.setId(3L);
        existing.setAssignment(assignment);
        existing.setStudent(Fixtures.student());
        existing.setNotes("ready");
        existing.setSubmittedAt(Instant.now());
        when(submissionRepository.findByAssignmentId(1L)).thenReturn(List.of(existing));
        assertThat(service.submissions(1L, 1L)).hasSize(1);
        when(assignmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submissions(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createsAndUpdatesMarkSubmission() {
        Assignment assignment = assignment(1L, Fixtures.schoolClass());
        when(assignmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(assignment));
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.empty());
        service.submitMarks(1L, 3L, 1L, 1L, BigDecimal.valueOf(75));
        verify(submissionRepository).save(any(AssignmentSubmission.class));

        AssignmentSubmission existing = new AssignmentSubmission();
        when(submissionRepository.findByAssignmentIdAndStudentId(1L, 1L)).thenReturn(Optional.of(existing));
        service.submitMarks(1L, 3L, 1L, 1L, BigDecimal.valueOf(80));
        assertThat(existing.getMarksObtained()).isEqualByComparingTo("80");

        when(assignmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submitMarks(1L, 3L, 9L, 1L, BigDecimal.ONE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRequiresAllocatedTeacher() {
        when(teacherService.requireByUser(3L)).thenReturn(Fixtures.teacher());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        org.mockito.Mockito.doThrow(new tz.co.chambaka.school.management.exception.BusinessException(
                        "Only the allocated teacher of this subject for this class can create assignments."))
                .when(allocationService)
                .requireTeachesForUser(1L, 3L, 1L, 1L, 1L, 1L, "create assignments");
        assertThatThrownBy(() -> service.create(1L, 3L, new AssignmentRequest(
                1L, 1L, 1L, "Algebra", "Questions 1-5", LocalDate.of(2026, 9, 20))))
                .isInstanceOf(tz.co.chambaka.school.management.exception.BusinessException.class)
                .hasMessageContaining("allocated teacher");
    }

    private Assignment assignment(Long id, tz.co.chambaka.school.management.model.SchoolClass schoolClass) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setTeacher(Fixtures.teacher());
        assignment.setSchoolClass(schoolClass);
        assignment.setSection(Fixtures.section());
        assignment.setSubject(Fixtures.subject());
        assignment.setTitle("Algebra");
        assignment.setInstructions("Do it");
        assignment.setDueDate(LocalDate.of(2026, 9, 20));
        assignment.setPublishedAt(Instant.now());
        return assignment;
    }
}
