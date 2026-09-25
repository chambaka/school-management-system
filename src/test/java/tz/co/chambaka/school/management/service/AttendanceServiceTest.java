package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.attendance.MarkStudentAttendanceRequest;
import tz.co.chambaka.school.management.dto.attendance.MarkTeacherAttendanceRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentAttendance;
import tz.co.chambaka.school.management.model.TeacherAttendance;
import tz.co.chambaka.school.management.model.enums.AttendanceStatus;
import tz.co.chambaka.school.management.repository.StudentAttendanceRepository;
import tz.co.chambaka.school.management.repository.TeacherAttendanceRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private StudentAttendanceRepository studentAttendanceRepository;
    @Mock
    private TeacherAttendanceRepository teacherAttendanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private TeacherService teacherService;
    @Mock
    private SectionService sectionService;
    @Mock
    private AlertService alertService;
    @Mock
    private tz.co.chambaka.school.management.repository.StudentRepository studentRepository;
    @Mock
    private tz.co.chambaka.school.management.repository.TimetableSlotRepository timetableSlotRepository;
    @InjectMocks
    private AttendanceService service;

    @Test
    void markAndQuery() {
        LocalDate date = LocalDate.of(2026, 9, 5);
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(userRepository.findById(2L)).thenReturn(Optional.of(Fixtures.user(2L, tz.co.chambaka.school.management.model.enums.Role.HEADMASTER)));
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(studentAttendanceRepository.findByStudentIdAndAttendanceDate(1L, date)).thenReturn(Optional.empty());
        when(studentAttendanceRepository.save(any(StudentAttendance.class))).thenAnswer(inv -> {
            StudentAttendance saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        var studentReq = new MarkStudentAttendanceRequest(1L, date,
                List.of(new MarkStudentAttendanceRequest.StudentAttendanceItem(1L, AttendanceStatus.PRESENT, null)));
        assertThat(service.markStudents(1L, studentReq, 2L).getFirst().status()).isEqualTo(AttendanceStatus.PRESENT);

        when(studentAttendanceRepository.findByStudentIdAndAttendanceDate(1L, date))
                .thenReturn(Optional.of(studentRow(date)));
        service.markStudents(1L, studentReq, 2L);

        when(teacherService.require(1L, 1L)).thenReturn(Fixtures.teacher());
        when(teacherAttendanceRepository.findByTeacherIdAndAttendanceDate(1L, date)).thenReturn(Optional.empty());
        when(teacherAttendanceRepository.save(any(TeacherAttendance.class))).thenAnswer(inv -> {
            TeacherAttendance saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        var teacherReq = new MarkTeacherAttendanceRequest(date,
                List.of(new MarkTeacherAttendanceRequest.TeacherAttendanceItem(1L, AttendanceStatus.LATE, "late")));
        assertThat(service.markTeachers(1L, teacherReq, 2L).getFirst().status()).isEqualTo(AttendanceStatus.LATE);

        when(studentAttendanceRepository.findBySchoolIdAndSectionIdAndAttendanceDate(1L, 1L, date))
                .thenReturn(List.of(studentRow(date)));
        when(teacherAttendanceRepository.findBySchoolIdAndAttendanceDate(1L, date))
                .thenReturn(List.of(teacherRow(date)));
        assertThat(service.dailyStudents(1L, 1L, date)).hasSize(1);
        assertThat(service.dailyTeachers(1L, date)).hasSize(1);

        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(studentAttendanceRepository.findBySchoolIdAndStudentIdAndAttendanceDateBetween(1L, 1L, date, date))
                .thenReturn(List.of(studentRow(date)));
        assertThat(service.studentRange(1L, 1L, date, date)).hasSize(1);
        when(studentAttendanceRepository.countByStudentIdAndAttendanceDateBetweenAndStatus(1L, date, date, AttendanceStatus.PRESENT))
                .thenReturn(8L);
        when(studentAttendanceRepository.countByStudentIdAndAttendanceDateBetweenAndStatus(1L, date, date, AttendanceStatus.ABSENT))
                .thenReturn(1L);
        when(studentAttendanceRepository.countByStudentIdAndAttendanceDateBetweenAndStatus(1L, date, date, AttendanceStatus.LATE))
                .thenReturn(1L);
        when(studentAttendanceRepository.countByStudentIdAndAttendanceDateBetweenAndStatus(1L, date, date, AttendanceStatus.EXCUSED))
                .thenReturn(0L);
        when(studentAttendanceRepository.countByStudentIdAndAttendanceDateBetween(1L, date, date)).thenReturn(10L);
        assertThat(service.studentSummary(1L, 1L, date, date).attendancePercent()).isEqualTo(90.0);

        when(studentAttendanceRepository.countByStudentIdAndAttendanceDateBetween(1L, date, date)).thenReturn(0L);
        assertThat(service.studentSummary(1L, 1L, date, date).attendancePercent()).isEqualTo(0.0);

        when(studentRepository.findBySchoolIdOrderByAdmissionNoAsc(1L)).thenReturn(List.of(Fixtures.student()));
        assertThat(service.schoolSummaries(1L, date, date)).hasSize(1);

        when(studentAttendanceRepository.findBySchoolIdAndSectionIdAndAttendanceDateAndTimetableSlotId(1L, 1L, date, 9L))
                .thenReturn(List.of(studentRow(date)));
        assertThat(service.dailyStudents(1L, 1L, date, 9L)).hasSize(1);
    }

    @Test
    void marksPerLessonSlot() {
        LocalDate date = LocalDate.of(2026, 9, 24);
        tz.co.chambaka.school.management.model.TimetableSlot slot = new tz.co.chambaka.school.management.model.TimetableSlot();
        slot.setId(9L);
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(userRepository.findById(2L)).thenReturn(Optional.of(Fixtures.user(2L, tz.co.chambaka.school.management.model.enums.Role.TEACHER)));
        when(timetableSlotRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.of(slot));
        when(studentService.require(1L, 1L)).thenReturn(Fixtures.student());
        when(studentAttendanceRepository.findByStudentIdAndAttendanceDateAndTimetableSlotId(1L, date, 9L))
                .thenReturn(Optional.empty());
        when(studentAttendanceRepository.save(any(StudentAttendance.class))).thenAnswer(inv -> {
            StudentAttendance saved = inv.getArgument(0);
            saved.setId(4L);
            return saved;
        });
        var req = new MarkStudentAttendanceRequest(1L, date,
                List.of(new MarkStudentAttendanceRequest.StudentAttendanceItem(1L, AttendanceStatus.PRESENT, null)), 9L);
        assertThat(service.markStudents(1L, req, 2L).getFirst().timetableSlotId()).isEqualTo(9L);
        when(timetableSlotRepository.findByIdAndSchoolId(8L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markStudents(1L, new MarkStudentAttendanceRequest(1L, date,
                List.of(new MarkStudentAttendanceRequest.StudentAttendanceItem(1L, AttendanceStatus.PRESENT, null)), 8L), 2L))
                .isInstanceOf(tz.co.chambaka.school.management.exception.ResourceNotFoundException.class);
    }

    @Test
    void rejectsStudentOutsideSection() {
        Student student = Fixtures.student();
        student.setSection(null);
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        when(studentService.require(1L, 1L)).thenReturn(student);
        var req = new MarkStudentAttendanceRequest(1L, LocalDate.now(),
                List.of(new MarkStudentAttendanceRequest.StudentAttendanceItem(1L, AttendanceStatus.ABSENT, null)));
        assertThatThrownBy(() -> service.markStudents(1L, req, 2L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void absenceNotifiesParents() {
        LocalDate date = LocalDate.of(2026, 9, 16);
        Student student = Fixtures.student();
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        when(studentService.require(1L, 1L)).thenReturn(student);
        when(studentAttendanceRepository.findByStudentIdAndAttendanceDate(1L, date)).thenReturn(Optional.empty());
        when(studentAttendanceRepository.save(any(StudentAttendance.class))).thenAnswer(invocation -> {
            StudentAttendance saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        service.markStudents(1L, new MarkStudentAttendanceRequest(1L, date,
                List.of(new MarkStudentAttendanceRequest.StudentAttendanceItem(
                        1L, AttendanceStatus.ABSENT, "Unexcused"))), 2L);

        verify(alertService).notifyParentsOfStudent(
                1L, student, "Attendance: ABSENT",
                "User STUDENT was marked absent on 2026-09-16", "ATTENDANCE", true);
    }

    private StudentAttendance studentRow(LocalDate date) {
        StudentAttendance row = new StudentAttendance();
        row.setId(1L);
        row.setStudent(Fixtures.student());
        row.setSection(Fixtures.section());
        row.setAttendanceDate(date);
        row.setStatus(AttendanceStatus.PRESENT);
        return row;
    }

    private TeacherAttendance teacherRow(LocalDate date) {
        TeacherAttendance row = new TeacherAttendance();
        row.setId(1L);
        row.setTeacher(Fixtures.teacher());
        row.setAttendanceDate(date);
        row.setStatus(AttendanceStatus.LATE);
        return row;
    }
}
