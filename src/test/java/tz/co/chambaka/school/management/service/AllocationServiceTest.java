package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AllocationRequest;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.TeacherSubject;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock
    private TeacherSubjectRepository teacherSubjectRepository;
    @Mock
    private TeacherService teacherService;
    @Mock
    private SubjectService subjectService;
    @Mock
    private ClassService classService;
    @Mock
    private SectionService sectionService;
    @Mock
    private AcademicYearService academicYearService;
    @InjectMocks
    private AllocationService service;

    @Test
    void listCreateDelete() {
        TeacherSubject withSection = allocation(true);
        TeacherSubject without = allocation(false);
        when(teacherSubjectRepository.findBySchoolIdAndTeacherId(1L, 1L)).thenReturn(List.of(withSection));
        when(teacherSubjectRepository.findBySchoolIdAndAcademicYearId(1L, 1L)).thenReturn(List.of(without));
        when(teacherSubjectRepository.findBySchoolId(1L)).thenReturn(List.of(withSection, without));
        assertThat(service.list(1L, 1L, 1L).getFirst().sectionName()).isEqualTo("A");
        assertThat(service.list(1L, 1L, null).getFirst().sectionId()).isNull();
        assertThat(service.list(1L, null, null)).hasSize(2);

        when(teacherSubjectRepository.existsByTeacherIdAndSubjectIdAndSchoolClassIdAndSectionIdAndAcademicYearId(
                1L, 1L, 1L, 1L, 1L)).thenReturn(false);
        when(teacherService.require(1L, 1L)).thenReturn(Fixtures.teacher());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(teacherSubjectRepository.save(any(TeacherSubject.class))).thenAnswer(inv -> {
            TeacherSubject saved = inv.getArgument(0);
            saved.setId(3L);
            return saved;
        });
        assertThat(service.create(1L, new AllocationRequest(1L, 1L, 1L, 1L, 1L)).id()).isEqualTo(3L);

        when(teacherSubjectRepository.existsByTeacherIdAndSubjectIdAndSchoolClassIdAndSectionIdAndAcademicYearId(
                1L, 1L, 1L, null, 1L)).thenReturn(false);
        service.create(1L, new AllocationRequest(1L, 1L, 1L, null, 1L));

        when(teacherSubjectRepository.findByIdAndSchoolId(3L, 1L)).thenReturn(Optional.of(withSection));
        service.delete(1L, 3L);
        verify(teacherSubjectRepository).delete(withSection);
    }

    @Test
    void errors() {
        when(teacherSubjectRepository.existsByTeacherIdAndSubjectIdAndSchoolClassIdAndSectionIdAndAcademicYearId(
                1L, 1L, 1L, 1L, 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new AllocationRequest(1L, 1L, 1L, 1L, 1L)))
                .isInstanceOf(DuplicateResourceException.class);
        when(teacherSubjectRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private TeacherSubject allocation(boolean withSection) {
        TeacherSubject allocation = new TeacherSubject();
        allocation.setId(1L);
        allocation.setTeacher(Fixtures.teacher());
        allocation.setSubject(Fixtures.subject());
        allocation.setSchoolClass(Fixtures.schoolClass());
        allocation.setAcademicYear(Fixtures.year());
        allocation.setSection(withSection ? Fixtures.section() : null);
        return allocation;
    }
}
