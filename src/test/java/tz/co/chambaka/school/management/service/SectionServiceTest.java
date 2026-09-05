package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.SectionRequest;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Section;
import tz.co.chambaka.school.management.repository.SectionRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private ClassService classService;
    @Mock
    private TeacherRepository teacherRepository;
    @InjectMocks
    private SectionService service;

    @Test
    void listCreateWithTeacherAndWithout() {
        Section section = Fixtures.section();
        when(sectionRepository.findBySchoolIdAndSchoolClassIdOrderByNameAsc(1L, 1L)).thenReturn(List.of(section));
        assertThat(service.list(1L, 1L).getFirst().classTeacherName()).isEqualTo("User TEACHER");

        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(sectionRepository.existsBySchoolClassIdAndNameIgnoreCase(1L, "B")).thenReturn(false);
        when(teacherRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(Fixtures.teacher()));
        when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> {
            Section saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        assertThat(service.create(1L, new SectionRequest(1L, "B", 30, 1L)).classTeacherId()).isEqualTo(1L);

        when(sectionRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(section));
        assertThat(service.update(1L, 1L, new SectionRequest(1L, "A", 45, null)).classTeacherId()).isNull();
    }

    @Test
    void errors() {
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(sectionRepository.existsBySchoolClassIdAndNameIgnoreCase(1L, "A")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new SectionRequest(1L, "A", 10, null)))
                .isInstanceOf(DuplicateResourceException.class);
        when(teacherRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        when(sectionRepository.existsBySchoolClassIdAndNameIgnoreCase(1L, "C")).thenReturn(false);
        assertThatThrownBy(() -> service.create(1L, new SectionRequest(1L, "C", 10, 9L)))
                .isInstanceOf(ResourceNotFoundException.class);
        when(sectionRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
