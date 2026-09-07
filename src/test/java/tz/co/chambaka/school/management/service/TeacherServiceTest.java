package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.teacher.CreateTeacherRequest;
import tz.co.chambaka.school.management.dto.teacher.UpdateTeacherRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.TeacherStatus;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private DepartmentService departmentService;
    @Mock
    private PhotoStorageService photoStorageService;
    @InjectMocks
    private TeacherService service;

    @Test
    void listGetCreateUpdate() {
        Teacher teacher = Fixtures.teacher();
        when(teacherRepository.search(1L, false, TeacherStatus.ARCHIVED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(teacher)));
        assertThat(service.list(1L, false, PageRequest.of(0, 10)).totalElements()).isEqualTo(1);

        when(teacherRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(teacher));
        assertThat(service.get(1L, 1L).employeeId()).isEqualTo("T-001");

        when(teacherRepository.existsBySchoolIdAndEmployeeIdIgnoreCase(1L, "T-002")).thenReturn(false);
        when(userAccountService.create(1L, "Asha", "asha@x.com", "pw", Role.TEACHER, "07"))
                .thenReturn(Fixtures.user(3L, Role.TEACHER));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> {
            Teacher saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(departmentService.requireByName(1L, "Science")).thenReturn(Fixtures.department("Science"));
        when(departmentService.requireByName(1L, "Arts")).thenReturn(Fixtures.department("Arts"));
        CreateTeacherRequest create = new CreateTeacherRequest(
                "Asha", "asha@x.com", "pw", "07", "T-002", "BSc", "Math", "Science", LocalDate.of(2024, 1, 1));
        assertThat(service.create(1L, create).id()).isEqualTo(2L);

        service.update(1L, 1L, new UpdateTeacherRequest("New", "08", "MSc", "Phy", "Arts",
                LocalDate.of(2025, 1, 1), false));
        assertThat(teacher.getUser().getName()).isEqualTo("New");
        assertThat(teacher.getUser().isEnabled()).isFalse();
        assertThat(teacher.getDepartment()).isEqualTo("Arts");

        service.archive(1L, 1L);
        assertThat(teacher.getStatus()).isEqualTo(TeacherStatus.ARCHIVED);
        assertThat(teacher.getUser().isEnabled()).isFalse();
        service.restore(1L, 1L);
        assertThat(teacher.getStatus()).isEqualTo(TeacherStatus.ACTIVE);
        assertThat(teacher.getUser().isEnabled()).isTrue();
        when(teacherRepository.search(1L, true, TeacherStatus.ARCHIVED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(service.list(1L, true, PageRequest.of(0, 10)).content()).isEmpty();
    }

    @Test
    void rejectsUnknownDepartmentAndClearsBlank() {
        Teacher teacher = Fixtures.teacher();
        when(teacherRepository.existsBySchoolIdAndEmployeeIdIgnoreCase(1L, "T-003")).thenReturn(false);
        when(userAccountService.create(1L, "Asha", "asha@x.com", "pw", Role.TEACHER, null))
                .thenReturn(Fixtures.user(3L, Role.TEACHER));
        when(departmentService.requireByName(1L, "Unknown"))
                .thenThrow(new BusinessException("Department is not configured for this school"));
        assertThatThrownBy(() -> service.create(1L, new CreateTeacherRequest(
                "Asha", "asha@x.com", "pw", null, "T-003", null, null, "Unknown", null)))
                .isInstanceOf(BusinessException.class);

        when(teacherRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(teacher));
        service.update(1L, 1L, new UpdateTeacherRequest(null, null, null, null, "  ", null, null));
        assertThat(teacher.getDepartment()).isNull();
    }

    @Test
    void lookupsAndErrors() {
        Teacher teacher = Fixtures.teacher();
        when(teacherRepository.existsBySchoolIdAndEmployeeIdIgnoreCase(1L, "T-001")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new CreateTeacherRequest(
                "A", "a@b.com", "pw", null, "T-001", null, null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
        when(teacherRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        when(teacherRepository.findByUserId(3L)).thenReturn(Optional.of(teacher));
        assertThat(service.requireByUser(3L)).isSameAs(teacher);
        when(teacherRepository.findByUserId(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireByUser(8L)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.requireByUserSafe(8L)).isNull();
    }

    @Test
    void uploadAndDeletePhoto() {
        Teacher teacher = Fixtures.teacher();
        when(teacherRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(teacher));
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2});
        when(photoStorageService.storeTeacherPhoto(1L, 1L, file)).thenReturn(Path.of("x.jpg"));
        assertThat(service.uploadPhoto(1L, 1L, file).photoUrl()).isEqualTo("/api/v1/teachers/1/photo");
        assertThat(teacher.getUser().getAvatarUrl()).isEqualTo("/api/v1/teachers/1/photo");

        when(photoStorageService.findTeacherPhoto(1L, 1L))
                .thenReturn(Optional.of(new StoredPhoto(Path.of("x.jpg"), "image/jpeg")));
        assertThat(service.photoFile(1L, 1L).contentType()).isEqualTo("image/jpeg");

        when(photoStorageService.findTeacherPhoto(1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.photoFile(1L, 1L)).isInstanceOf(ResourceNotFoundException.class);

        assertThat(service.deletePhoto(1L, 1L).photoUrl()).isNull();
        assertThat(teacher.getUser().getAvatarUrl()).isNull();
        verify(photoStorageService).deleteTeacherPhoto(1L, 1L);
        assertThat(TeacherService.photoPath(7L)).isEqualTo("/api/v1/teachers/7/photo");
    }
}
