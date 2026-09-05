package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.attendance.AttendanceSummaryResponse;
import tz.co.chambaka.school.management.dto.attendance.MarkStudentAttendanceRequest;
import tz.co.chambaka.school.management.dto.attendance.MarkTeacherAttendanceRequest;
import tz.co.chambaka.school.management.dto.attendance.StudentAttendanceResponse;
import tz.co.chambaka.school.management.dto.attendance.TeacherAttendanceResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentAttendance;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.TeacherAttendance;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.AttendanceStatus;
import tz.co.chambaka.school.management.repository.StudentAttendanceRepository;
import tz.co.chambaka.school.management.repository.TeacherAttendanceRepository;
import tz.co.chambaka.school.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final StudentAttendanceRepository studentAttendanceRepository;
    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final UserRepository userRepository;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final SectionService sectionService;

    public AttendanceService(
            StudentAttendanceRepository studentAttendanceRepository,
            TeacherAttendanceRepository teacherAttendanceRepository,
            UserRepository userRepository,
            StudentService studentService,
            TeacherService teacherService,
            SectionService sectionService
    ) {
        this.studentAttendanceRepository = studentAttendanceRepository;
        this.teacherAttendanceRepository = teacherAttendanceRepository;
        this.userRepository = userRepository;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.sectionService = sectionService;
    }

    @Transactional
    public List<StudentAttendanceResponse> markStudents(Long schoolId, MarkStudentAttendanceRequest request, Long markedBy) {
        Section section = sectionService.require(schoolId, request.sectionId());
        User marker = userRepository.findById(markedBy).orElse(null);
        List<StudentAttendanceResponse> marked = request.entries().stream().map(entry -> {
            Student student = studentService.require(schoolId, entry.studentId());
            if (student.getSection() == null || !student.getSection().getId().equals(section.getId())) {
                throw new BusinessException("Student " + student.getAdmissionNo() + " is not in this section");
            }
            StudentAttendance attendance = studentAttendanceRepository
                    .findByStudentIdAndAttendanceDate(student.getId(), request.date())
                    .orElseGet(StudentAttendance::new);
            attendance.setSchoolId(schoolId);
            attendance.setStudent(student);
            attendance.setSection(section);
            attendance.setAttendanceDate(request.date());
            attendance.setStatus(entry.status());
            attendance.setRemarks(entry.remarks());
            attendance.setMarkedBy(marker);
            return toStudent(studentAttendanceRepository.save(attendance));
        }).toList();
        log.info("Marked student attendance schoolId={} sectionId={} date={} count={}",
                schoolId, request.sectionId(), request.date(), marked.size());
        return marked;
    }

    @Transactional
    public List<TeacherAttendanceResponse> markTeachers(Long schoolId, MarkTeacherAttendanceRequest request, Long markedBy) {
        User marker = userRepository.findById(markedBy).orElse(null);
        List<TeacherAttendanceResponse> marked = request.entries().stream().map(entry -> {
            Teacher teacher = teacherService.require(schoolId, entry.teacherId());
            TeacherAttendance attendance = teacherAttendanceRepository
                    .findByTeacherIdAndAttendanceDate(teacher.getId(), request.date())
                    .orElseGet(TeacherAttendance::new);
            attendance.setSchoolId(schoolId);
            attendance.setTeacher(teacher);
            attendance.setAttendanceDate(request.date());
            attendance.setStatus(entry.status());
            attendance.setRemarks(entry.remarks());
            attendance.setMarkedBy(marker);
            return toTeacher(teacherAttendanceRepository.save(attendance));
        }).toList();
        log.info("Marked teacher attendance schoolId={} date={} count={}", schoolId, request.date(), marked.size());
        return marked;
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceResponse> dailyStudents(Long schoolId, Long sectionId, LocalDate date) {
        return studentAttendanceRepository.findBySchoolIdAndSectionIdAndAttendanceDate(schoolId, sectionId, date)
                .stream().map(this::toStudent).toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherAttendanceResponse> dailyTeachers(Long schoolId, LocalDate date) {
        return teacherAttendanceRepository.findBySchoolIdAndAttendanceDate(schoolId, date)
                .stream().map(this::toTeacher).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse studentSummary(Long schoolId, Long studentId, LocalDate start, LocalDate end) {
        Student student = studentService.require(schoolId, studentId);
        return buildSummary(student.getId(), student.getUser().getName(), start, end, true);
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceResponse> studentRange(Long schoolId, Long studentId, LocalDate start, LocalDate end) {
        studentService.require(schoolId, studentId);
        return studentAttendanceRepository
                .findBySchoolIdAndStudentIdAndAttendanceDateBetween(schoolId, studentId, start, end)
                .stream().map(this::toStudent).toList();
    }

    private AttendanceSummaryResponse buildSummary(Long personId, String name, LocalDate start, LocalDate end, boolean student) {
        long present = studentAttendanceRepository.countByStudentIdAndAttendanceDateBetweenAndStatus(
                personId, start, end, AttendanceStatus.PRESENT);
        long absent = studentAttendanceRepository.countByStudentIdAndAttendanceDateBetweenAndStatus(
                personId, start, end, AttendanceStatus.ABSENT);
        long late = studentAttendanceRepository.countByStudentIdAndAttendanceDateBetweenAndStatus(
                personId, start, end, AttendanceStatus.LATE);
        long excused = studentAttendanceRepository.countByStudentIdAndAttendanceDateBetweenAndStatus(
                personId, start, end, AttendanceStatus.EXCUSED);
        long total = studentAttendanceRepository.countByStudentIdAndAttendanceDateBetween(personId, start, end);
        double percent = total == 0 ? 0 : ((present + late) * 100.0) / total;
        return new AttendanceSummaryResponse(personId, name, present, absent, late, excused, total,
                Math.round(percent * 100.0) / 100.0);
    }

    private StudentAttendanceResponse toStudent(StudentAttendance attendance) {
        return new StudentAttendanceResponse(
                attendance.getId(),
                attendance.getStudent().getId(),
                attendance.getStudent().getUser().getName(),
                attendance.getSection().getId(),
                attendance.getAttendanceDate(),
                attendance.getStatus(),
                attendance.getRemarks()
        );
    }

    private TeacherAttendanceResponse toTeacher(TeacherAttendance attendance) {
        return new TeacherAttendanceResponse(
                attendance.getId(),
                attendance.getTeacher().getId(),
                attendance.getTeacher().getUser().getName(),
                attendance.getAttendanceDate(),
                attendance.getStatus(),
                attendance.getRemarks()
        );
    }
}
