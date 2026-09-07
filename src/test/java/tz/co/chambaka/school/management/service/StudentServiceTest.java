package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.student.CreateStudentRequest;
import tz.co.chambaka.school.management.dto.student.StudentResponse;
import tz.co.chambaka.school.management.dto.student.UpdateStudentRequest;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.enums.Gender;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.model.enums.StudentStatus;
import tz.co.chambaka.school.management.repository.StudentParentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
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
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentParentRepository studentParentRepository;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private ClassService classService;
    @Mock
    private SectionService sectionService;
    @Mock
    private PhotoStorageService photoStorageService;
    @InjectMocks
    private StudentService service;

    @Test
    void listGetCreateUpdate() {
        Student student = Fixtures.student();
        when(studentRepository.search(1L, null, false, StudentStatus.ARCHIVED, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(student)));
        when(studentRepository.search(1L, 1L, false, StudentStatus.ARCHIVED, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(student)));
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of());
        assertThat(service.list(1L, null, false, PageRequest.of(0, 5)).content()).hasSize(1);
        assertThat(service.list(1L, 1L, false, PageRequest.of(0, 5)).content().getFirst().sectionName()).isEqualTo("A");

        when(studentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(student));
        assertThat(service.get(1L, 1L).admissionNo()).isEqualTo("ADM-001");

        when(studentRepository.countBySchoolIdAndAdmissionNoStartingWithIgnoreCase(any(), any())).thenReturn(1L);
        when(studentRepository.existsBySchoolIdAndAdmissionNoIgnoreCase(any(), any())).thenReturn(false);
        when(userAccountService.create(1L, "Juma", "j@x.com", "pw", Role.STUDENT, "07"))
                .thenReturn(Fixtures.user(4L, Role.STUDENT));
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        CreateStudentRequest create = new CreateStudentRequest(
                "Juma", "j@x.com", "pw", "07", "1", LocalDate.of(2010, 1, 1),
                Gender.MALE, "O+", LocalDate.of(2026, 1, 1), "addr", "0711", 1L, 1L, 1L);
        StudentResponse created = service.create(1L, create);
        assertThat(created.id()).isEqualTo(2L);
        assertThat(created.admissionNo()).startsWith("ADM-");

        service.update(1L, 1L, new UpdateStudentRequest(
                "Juma 2", "08", "2", LocalDate.of(2011, 1, 1), Gender.FEMALE, "A+",
                "new", "0722", 1L, 1L, 1L, false));
        assertThat(student.getUser().getName()).isEqualTo("Juma 2");
        assertThat(student.getUser().isEnabled()).isFalse();
        assertThat(student.getGender()).isEqualTo(Gender.FEMALE);

        service.suspend(1L, 1L);
        assertThat(student.getStatus()).isEqualTo(StudentStatus.SUSPENDED);
        assertThat(student.getUser().isEnabled()).isFalse();
        service.archive(1L, 1L);
        assertThat(student.getStatus()).isEqualTo(StudentStatus.ARCHIVED);
        service.restore(1L, 1L);
        assertThat(student.getStatus()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(student.getUser().isEnabled()).isTrue();
        when(studentRepository.search(1L, null, true, StudentStatus.ARCHIVED, PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of()));
        assertThat(service.list(1L, null, true, PageRequest.of(0, 5)).content()).isEmpty();
    }

    @Test
    void createWithoutPlacementAndErrors() {
        when(studentRepository.countBySchoolIdAndAdmissionNoStartingWithIgnoreCase(any(), any())).thenReturn(0L);
        when(studentRepository.existsBySchoolIdAndAdmissionNoIgnoreCase(any(), any())).thenReturn(true, false);
        when(userAccountService.create(1L, "N", "n@x.com", "pw", Role.STUDENT, null))
                .thenReturn(Fixtures.user(4L, Role.STUDENT));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateStudentRequest bare = new CreateStudentRequest(
                "N", "n@x.com", "pw", null, null, null, null, null, null, null, null, null, null, null);
        assertThat(service.create(1L, bare).schoolClassId()).isNull();
        assertThat(service.create(1L, bare).admissionNo()).startsWith("ADM-");

        when(studentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        when(studentRepository.findByUserId(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireByUser(8L)).isInstanceOf(ResourceNotFoundException.class);
        when(studentRepository.findByUserId(4L)).thenReturn(Optional.of(Fixtures.student()));
        assertThat(service.requireByUser(4L).getAdmissionNo()).isEqualTo("ADM-001");
    }

    @Test
    void uploadAndDeletePhoto() {
        Student student = Fixtures.student();
        when(studentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(student));
        when(studentParentRepository.findByStudentId(1L)).thenReturn(List.of());
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2});
        when(photoStorageService.storeStudentPhoto(1L, 1L, file)).thenReturn(Path.of("x.jpg"));
        assertThat(service.uploadPhoto(1L, 1L, file).photoUrl()).isEqualTo("/api/v1/students/1/photo");
        assertThat(student.getUser().getAvatarUrl()).isEqualTo("/api/v1/students/1/photo");

        when(photoStorageService.findStudentPhoto(1L, 1L))
                .thenReturn(Optional.of(new StoredPhoto(Path.of("x.jpg"), "image/jpeg")));
        assertThat(service.photoFile(1L, 1L).contentType()).isEqualTo("image/jpeg");

        when(photoStorageService.findStudentPhoto(1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.photoFile(1L, 1L)).isInstanceOf(ResourceNotFoundException.class);

        assertThat(service.deletePhoto(1L, 1L).photoUrl()).isNull();
        assertThat(student.getUser().getAvatarUrl()).isNull();
        verify(photoStorageService).deleteStudentPhoto(1L, 1L);
        assertThat(StudentService.photoPath(7L)).isEqualTo("/api/v1/students/7/photo");
    }
}
