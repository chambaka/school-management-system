package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.admin.RemoveUserLinkRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.StudentParent;
import tz.co.chambaka.school.management.model.TeacherSubject;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.RelationshipType;
import tz.co.chambaka.school.management.model.enums.Role;
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
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLinkServiceTest {

    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private StudentParentRepository studentParentRepository;
    @Mock private TeacherSubjectRepository teacherSubjectRepository;
    @Mock private NoticeRepository noticeRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private StudentAttendanceRepository studentAttendanceRepository;
    @Mock private TeacherAttendanceRepository teacherAttendanceRepository;
    @Mock private StudentCommunicationRepository studentCommunicationRepository;
    @InjectMocks private UserLinkService service;

    @Test
    void listsTeacherAllocationsAndActivityCounts() {
        User user = Fixtures.user(3L, Role.TEACHER);
        when(teacherRepository.findByUserId(3L)).thenReturn(Optional.of(Fixtures.teacher()));
        when(studentRepository.findByUserId(3L)).thenReturn(Optional.empty());
        when(parentRepository.findByUserId(3L)).thenReturn(Optional.empty());
        TeacherSubject allocation = new TeacherSubject();
        allocation.setId(8L);
        allocation.setSubject(Fixtures.subject());
        allocation.setSchoolClass(Fixtures.schoolClass());
        allocation.setAcademicYear(Fixtures.year());
        when(teacherSubjectRepository.findBySchoolIdAndTeacherId(1L, 1L)).thenReturn(List.of(allocation));
        when(noticeRepository.countByCreatedById(3L)).thenReturn(2L);
        when(paymentRepository.countByRecordedById(3L)).thenReturn(0L);
        when(studentAttendanceRepository.countByMarkedById(3L)).thenReturn(1L);
        when(teacherAttendanceRepository.countByMarkedById(3L)).thenReturn(0L);
        when(studentCommunicationRepository.countByAuthorId(3L)).thenReturn(0L);

        var response = service.forUser(user);

        assertThat(response.userId()).isEqualTo(3L);
        assertThat(response.links()).extracting("type").contains("Teacher", "Allocation", "Notice", "Student attendance");
        assertThat(response.links()).anyMatch(item -> "Mathematics".equals(item.title()) && item.removable());
        assertThat(response.links()).anyMatch(item -> "2 records".equals(item.detail()) && !item.removable());
    }

    @Test
    void listsStudentAndLinkedParent() {
        User user = Fixtures.user(4L, Role.STUDENT);
        when(teacherRepository.findByUserId(4L)).thenReturn(Optional.empty());
        when(studentRepository.findByUserId(4L)).thenReturn(Optional.of(Fixtures.student()));
        when(parentRepository.findByUserId(4L)).thenReturn(Optional.empty());
        StudentParent link = new StudentParent();
        link.setParent(Fixtures.parent());
        link.setRelationship(RelationshipType.MOTHER);
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of(link));
        stubZeroActivity(4L);

        var response = service.forUser(user);

        assertThat(response.links()).extracting("type").contains("Student", "Parent");
        assertThat(response.links()).anyMatch(item -> "ADM-001".equals(item.title()) && !item.removable());
        assertThat(response.links()).anyMatch(item -> "STUDENT_PARENT".equals(item.unlinkKind()) && item.parentId() == 1L);
    }

    @Test
    void listsParentAndChildren() {
        User user = Fixtures.user(5L, Role.PARENT);
        when(teacherRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(studentRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(parentRepository.findByUserId(5L)).thenReturn(Optional.of(Fixtures.parent()));
        StudentParent link = new StudentParent();
        link.setStudent(Fixtures.student());
        when(studentParentRepository.findByParentId(1L)).thenReturn(List.of(link));
        stubZeroActivity(5L);

        var response = service.forUser(user);

        assertThat(response.links()).extracting("type").contains("Parent", "Student");
        assertThat(response.links()).anyMatch(item -> "User STUDENT".equals(item.title()));
    }

    @Test
    void unusedStaffHasNoLinks() {
        User user = Fixtures.user(6L, Role.STAFF);
        when(teacherRepository.findByUserId(6L)).thenReturn(Optional.empty());
        when(studentRepository.findByUserId(6L)).thenReturn(Optional.empty());
        when(parentRepository.findByUserId(6L)).thenReturn(Optional.empty());
        stubZeroActivity(6L);

        assertThat(service.forUser(user).links()).isEmpty();
    }

    @Test
    void removesTeacherAllocation() {
        User user = Fixtures.user(3L, Role.TEACHER);
        TeacherSubject allocation = new TeacherSubject();
        allocation.setId(8L);
        allocation.setTeacher(Fixtures.teacher());
        when(teacherRepository.findByUserId(3L)).thenReturn(Optional.of(Fixtures.teacher()));
        when(teacherSubjectRepository.findByIdAndSchoolId(8L, 1L)).thenReturn(Optional.of(allocation));

        service.remove(user, new RemoveUserLinkRequest("ALLOCATION", 8L, null, null));

        verify(teacherSubjectRepository).delete(allocation);
    }

    @Test
    void removesStudentParentLink() {
        User user = Fixtures.user(4L, Role.STUDENT);
        StudentParent link = new StudentParent();
        when(studentRepository.findByUserId(4L)).thenReturn(Optional.of(Fixtures.student()));
        when(parentRepository.findByUserId(4L)).thenReturn(Optional.empty());
        when(studentParentRepository.findByStudentIdAndParentId(1L, 1L)).thenReturn(Optional.of(link));

        service.remove(user, new RemoveUserLinkRequest("STUDENT_PARENT", null, 1L, 1L));

        verify(studentParentRepository).delete(link);
    }

    @Test
    void removesParentChildLink() {
        User user = Fixtures.user(5L, Role.PARENT);
        StudentParent link = new StudentParent();
        when(studentRepository.findByUserId(5L)).thenReturn(Optional.empty());
        when(parentRepository.findByUserId(5L)).thenReturn(Optional.of(Fixtures.parent()));
        when(studentParentRepository.findByStudentIdAndParentId(1L, 1L)).thenReturn(Optional.of(link));

        service.remove(user, new RemoveUserLinkRequest("STUDENT_PARENT", null, 1L, 1L));

        verify(studentParentRepository).delete(link);
    }

    @Test
    void rejectsUnknownLinkKind() {
        User user = Fixtures.user(6L, Role.STAFF);
        assertThatThrownBy(() -> service.remove(user, new RemoveUserLinkRequest("Notice", null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsAllocationThatIsNotThisTeacher() {
        User user = Fixtures.user(3L, Role.TEACHER);
        TeacherSubject allocation = new TeacherSubject();
        allocation.setId(8L);
        allocation.setTeacher(Fixtures.teacher());
        allocation.getTeacher().setId(99L);
        when(teacherRepository.findByUserId(3L)).thenReturn(Optional.of(Fixtures.teacher()));
        when(teacherSubjectRepository.findByIdAndSchoolId(8L, 1L)).thenReturn(Optional.of(allocation));

        assertThatThrownBy(() -> service.remove(user, new RemoveUserLinkRequest("ALLOCATION", 8L, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not linked");
    }

    private void stubZeroActivity(Long userId) {
        when(noticeRepository.countByCreatedById(userId)).thenReturn(0L);
        when(paymentRepository.countByRecordedById(userId)).thenReturn(0L);
        when(studentAttendanceRepository.countByMarkedById(userId)).thenReturn(0L);
        when(teacherAttendanceRepository.countByMarkedById(userId)).thenReturn(0L);
        when(studentCommunicationRepository.countByAuthorId(userId)).thenReturn(0L);
    }
}
