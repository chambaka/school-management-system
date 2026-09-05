package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.SchoolClassRequest;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.repository.SchoolClassRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassServiceTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;
    @Mock
    private AcademicYearService academicYearService;
    @InjectMocks
    private ClassService service;

    @Test
    void listCreateUpdate() {
        SchoolClass schoolClass = Fixtures.schoolClass();
        when(schoolClassRepository.findBySchoolIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of(schoolClass));
        when(schoolClassRepository.findBySchoolIdAndAcademicYearIdOrderByDisplayOrderAsc(1L, 1L))
                .thenReturn(List.of(schoolClass));
        assertThat(service.list(1L, null)).hasSize(1);
        assertThat(service.list(1L, 1L).getFirst().code()).isEqualTo("F1");

        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(schoolClassRepository.existsBySchoolIdAndAcademicYearIdAndCodeIgnoreCase(1L, 1L, "F1")).thenReturn(false);
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(inv -> {
            SchoolClass saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        SchoolClassRequest req = new SchoolClassRequest(1L, "Form 1", "F1", 1);
        assertThat(service.create(1L, req).id()).isEqualTo(2L);

        when(schoolClassRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(schoolClass));
        assertThat(service.update(1L, 1L, new SchoolClassRequest(1L, "Form 1B", "F1B", 2)).name()).isEqualTo("Form 1B");
    }

    @Test
    void duplicateAndMissing() {
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(schoolClassRepository.existsBySchoolIdAndAcademicYearIdAndCodeIgnoreCase(1L, 1L, "F1")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new SchoolClassRequest(1L, "Form 1", "F1", 1)))
                .isInstanceOf(DuplicateResourceException.class);
        when(schoolClassRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
