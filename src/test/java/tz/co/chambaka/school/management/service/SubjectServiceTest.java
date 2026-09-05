package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.SubjectRequest;
import tz.co.chambaka.school.management.dto.academic.SubjectResponse;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.Subject;
import tz.co.chambaka.school.management.repository.SubjectRepository;
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
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private AcademicMapper academicMapper;
    @InjectMocks
    private SubjectService service;

    @Test
    void listCreateUpdate() {
        Subject subject = Fixtures.subject();
        SubjectResponse dto = new SubjectResponse(1L, "Mathematics", "MATH", null);
        when(subjectRepository.findBySchoolIdOrderByNameAsc(1L)).thenReturn(List.of(subject));
        when(academicMapper.toSubject(any(Subject.class))).thenReturn(dto);
        assertThat(service.list(1L)).hasSize(1);

        when(subjectRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "MATH")).thenReturn(false);
        when(subjectRepository.save(any(Subject.class))).thenReturn(subject);
        assertThat(service.create(1L, new SubjectRequest("Mathematics", "MATH", "desc")).code()).isEqualTo("MATH");

        when(subjectRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(subject));
        service.update(1L, 1L, new SubjectRequest("Math", "MTH", "d"));
        assertThat(subject.getName()).isEqualTo("Math");
    }

    @Test
    void errors() {
        when(subjectRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "MATH")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new SubjectRequest("Mathematics", "MATH", null)))
                .isInstanceOf(DuplicateResourceException.class);
        when(subjectRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
