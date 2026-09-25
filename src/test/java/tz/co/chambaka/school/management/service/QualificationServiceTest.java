package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.teacher.QualificationRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.TeacherQualification;
import tz.co.chambaka.school.management.repository.TeacherQualificationRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QualificationServiceTest {

    @Mock
    private TeacherQualificationRepository qualificationRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @InjectMocks
    private QualificationService service;

    @Test
    void listCreateUpdateAndDelete() {
        TeacherQualification existing = qualification("Diploma", 1);
        when(qualificationRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(existing));
        assertThat(service.list()).hasSize(1).first().extracting("name").isEqualTo("Diploma");

        when(qualificationRepository.existsByNameIgnoreCase("BEd")).thenReturn(false);
        when(qualificationRepository.save(any(TeacherQualification.class))).thenAnswer(inv -> {
            TeacherQualification saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        assertThat(service.create(new QualificationRequest(" BEd ", null)).name()).isEqualTo("BEd");

        when(qualificationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(qualificationRepository.existsByNameIgnoreCase("Bachelor of Education")).thenReturn(false);
        Teacher teacher = Fixtures.teacher();
        teacher.setQualification("Diploma");
        when(teacherRepository.findByQualificationIgnoreCase("Diploma")).thenReturn(List.of(teacher));
        assertThat(service.update(1L, new QualificationRequest("Bachelor of Education", 3)).sortOrder()).isEqualTo(3);
        assertThat(existing.getName()).isEqualTo("Bachelor of Education");
        assertThat(teacher.getQualification()).isEqualTo("Bachelor of Education");

        service.delete(1L);
        verify(qualificationRepository).delete(existing);
    }

    @Test
    void updateKeepsSortWhenOmitted() {
        TeacherQualification existing = qualification("Diploma", 4);
        when(qualificationRepository.findById(1L)).thenReturn(Optional.of(existing));
        assertThat(service.update(1L, new QualificationRequest("diploma", null)).sortOrder()).isEqualTo(4);
        assertThat(existing.getName()).isEqualTo("diploma");
    }

    @Test
    void errors() {
        when(qualificationRepository.existsByNameIgnoreCase("Diploma")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new QualificationRequest("Diploma", 0)))
                .isInstanceOf(DuplicateResourceException.class);

        TeacherQualification existing = qualification("Diploma", 0);
        when(qualificationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(qualificationRepository.existsByNameIgnoreCase("Degree")).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, new QualificationRequest("Degree", 1)))
                .isInstanceOf(DuplicateResourceException.class);

        when(qualificationRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(9L)).isInstanceOf(ResourceNotFoundException.class);

        when(qualificationRepository.findByNameIgnoreCase("Unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireByName(" Unknown "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Qualification is not configured");

        when(qualificationRepository.findByNameIgnoreCase("Diploma")).thenReturn(Optional.of(existing));
        assertThat(service.requireByName("Diploma")).isEqualTo("Diploma");
    }

    private static TeacherQualification qualification(String name, int sortOrder) {
        TeacherQualification qualification = new TeacherQualification();
        qualification.setId(1L);
        qualification.setName(name);
        qualification.setSortOrder(sortOrder);
        return qualification;
    }
}
