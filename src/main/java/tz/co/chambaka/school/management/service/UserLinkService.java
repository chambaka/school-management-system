package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.admin.UserLinkItem;
import tz.co.chambaka.school.management.dto.admin.UserLinksResponse;
import tz.co.chambaka.school.management.model.Parent;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.TeacherSubject;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.repository.NoticeRepository;
import tz.co.chambaka.school.management.repository.ParentRepository;
import tz.co.chambaka.school.management.repository.PaymentRepository;
import tz.co.chambaka.school.management.repository.StudentAttendanceRepository;
import tz.co.chambaka.school.management.repository.StudentCommunicationRepository;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherAttendanceRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserLinkService {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final NoticeRepository noticeRepository;
    private final PaymentRepository paymentRepository;
    private final StudentAttendanceRepository studentAttendanceRepository;
    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final StudentCommunicationRepository studentCommunicationRepository;

    public UserLinkService(
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            ParentRepository parentRepository,
            StudentParentRepository studentParentRepository,
            TeacherSubjectRepository teacherSubjectRepository,
            NoticeRepository noticeRepository,
            PaymentRepository paymentRepository,
            StudentAttendanceRepository studentAttendanceRepository,
            TeacherAttendanceRepository teacherAttendanceRepository,
            StudentCommunicationRepository studentCommunicationRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
        this.studentParentRepository = studentParentRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.noticeRepository = noticeRepository;
        this.paymentRepository = paymentRepository;
        this.studentAttendanceRepository = studentAttendanceRepository;
        this.teacherAttendanceRepository = teacherAttendanceRepository;
        this.studentCommunicationRepository = studentCommunicationRepository;
    }

    @Transactional(readOnly = true)
    public UserLinksResponse forUser(User user) {
        List<UserLinkItem> links = new ArrayList<>();
        teacherRepository.findByUserId(user.getId()).ifPresent(teacher -> addTeacher(links, teacher, user.getSchoolId()));
        studentRepository.findByUserId(user.getId()).ifPresent(student -> addStudent(links, student));
        parentRepository.findByUserId(user.getId()).ifPresent(parent -> addParent(links, parent));
        addCount(links, "Notice", noticeRepository.countByCreatedById(user.getId()), "Notices posted");
        addCount(links, "Payment", paymentRepository.countByRecordedById(user.getId()), "Payments recorded");
        addCount(links, "Student attendance", studentAttendanceRepository.countByMarkedById(user.getId()),
                "Student attendance marked");
        addCount(links, "Staff attendance", teacherAttendanceRepository.countByMarkedById(user.getId()),
                "Staff attendance marked");
        addCount(links, "Message", studentCommunicationRepository.countByAuthorId(user.getId()),
                "Student messages posted");
        return new UserLinksResponse(user.getId(), user.getName(), user.getRole(), List.copyOf(links));
    }

    private void addTeacher(List<UserLinkItem> links, Teacher teacher, Long schoolId) {
        links.add(new UserLinkItem(
                "Teacher",
                teacher.getId(),
                teacher.getEmployeeId() == null || teacher.getEmployeeId().isBlank() ? "Teacher profile" : teacher.getEmployeeId(),
                join(" · ", teacher.getDepartment(), teacher.getSpecialization())));
        if (schoolId == null) {
            return;
        }
        for (TeacherSubject allocation : teacherSubjectRepository.findBySchoolIdAndTeacherId(schoolId, teacher.getId())) {
            String subject = allocation.getSubject() == null ? "Subject" : allocation.getSubject().getName();
            String klass = allocation.getSchoolClass() == null ? null : allocation.getSchoolClass().getName();
            String section = allocation.getSection() == null ? null : allocation.getSection().getName();
            String year = allocation.getAcademicYear() == null ? null : allocation.getAcademicYear().getName();
            links.add(new UserLinkItem("Allocation", allocation.getId(), subject, join(" · ", klass, section, year)));
        }
    }

    private void addStudent(List<UserLinkItem> links, Student student) {
        String klass = student.getSchoolClass() == null ? null : student.getSchoolClass().getName();
        String section = student.getSection() == null ? null : student.getSection().getName();
        links.add(new UserLinkItem(
                "Student",
                student.getId(),
                student.getAdmissionNo() == null || student.getAdmissionNo().isBlank() ? "Student profile" : student.getAdmissionNo(),
                join(" · ", klass, section)));
        for (StudentParent link : studentParentRepository.findByStudentId(student.getId())) {
            Parent parent = link.getParent();
            String parentName = parent == null || parent.getUser() == null ? "Parent" : parent.getUser().getName();
            links.add(new UserLinkItem(
                    "Parent",
                    parent == null ? null : parent.getId(),
                    parentName,
                    link.getRelationship() == null ? "Linked parent" : link.getRelationship().name()));
        }
    }

    private void addParent(List<UserLinkItem> links, Parent parent) {
        links.add(new UserLinkItem("Parent", parent.getId(), "Parent profile", parent.getOccupation()));
        for (StudentParent link : studentParentRepository.findByParentId(parent.getId())) {
            Student student = link.getStudent();
            String name = student == null || student.getUser() == null ? "Student" : student.getUser().getName();
            String admission = student == null ? null : student.getAdmissionNo();
            links.add(new UserLinkItem("Student", student == null ? null : student.getId(), name, admission));
        }
    }

    private static void addCount(List<UserLinkItem> links, String type, long count, String title) {
        if (count <= 0) {
            return;
        }
        links.add(new UserLinkItem(type, null, title, count == 1 ? "1 record" : count + " records"));
    }

    private static String join(String sep, String... parts) {
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(sep);
            }
            out.append(part);
        }
        return out.isEmpty() ? null : out.toString();
    }
}
