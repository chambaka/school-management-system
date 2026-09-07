package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.DepartmentRequest;
import tz.co.chambaka.school.management.dto.academic.DepartmentResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.Department;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.repository.DepartmentRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private AcademicMapper academicMapper;
    @InjectMocks
    private DepartmentService service;

    @Test
    void listCreateUpdate() {
        Department department = Fixtures.department();
        DepartmentResponse dto = new DepartmentResponse(1L, "Science", null);
        when(departmentRepository.findBySchoolIdOrderByNameAsc(1L)).thenReturn(List.of(department));
        when(academicMapper.toDepartment(any(Department.class))).thenReturn(dto);
        assertThat(service.list(1L)).hasSize(1);

        when(departmentRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Science")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        assertThat(service.create(1L, new DepartmentRequest(" Science ", "labs")).name()).isEqualTo("Science");

        when(departmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(department));
        service.update(1L, 1L, new DepartmentRequest("Sciences", "labs"));
        assertThat(department.getName()).isEqualTo("Sciences");
        assertThat(department.getDescription()).isEqualTo("labs");
    }

    @Test
    void updateKeepsSameName() {
        Department department = Fixtures.department();
        when(departmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(department));
        when(academicMapper.toDepartment(department)).thenReturn(new DepartmentResponse(1L, "Science", "labs"));
        DepartmentResponse updated = service.update(1L, 1L, new DepartmentRequest("science", "labs"));
        assertThat(updated.name()).isEqualTo("Science");
        assertThat(department.getName()).isEqualTo("science");
    }

    @Test
    void errors() {
        when(departmentRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Science")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new DepartmentRequest("Science", null)))
                .isInstanceOf(DuplicateResourceException.class);
        Department existing = Fixtures.department();
        when(departmentRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Arts")).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, 1L, new DepartmentRequest("Arts", null)))
                .isInstanceOf(DuplicateResourceException.class);
        when(departmentRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
        when(departmentRepository.findBySchoolIdAndNameIgnoreCase(1L, "Missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireByName(1L, "Missing"))
                .isInstanceOf(BusinessException.class);
        when(departmentRepository.findBySchoolIdAndNameIgnoreCase(1L, "Science"))
                .thenReturn(Optional.of(Fixtures.department()));
        assertThat(service.requireByName(1L, " Science ").getName()).isEqualTo("Science");
    }

    @Test
    void ensureDefaultsCreatesMissingOnly() {
        when(departmentRepository.existsBySchoolIdAndNameIgnoreCase(any(), any())).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));
        service.ensureDefaults(1L);
        verify(departmentRepository, times(DepartmentService.DEFAULTS.size())).save(any(Department.class));

        when(departmentRepository.existsBySchoolIdAndNameIgnoreCase(any(), any())).thenReturn(true);
        service.ensureDefaults(1L);
        verify(departmentRepository, times(DepartmentService.DEFAULTS.size())).save(any(Department.class));
    }

    @Test
    void seedDefaultsSkipsArchivedSchools() {
        School live = Fixtures.school();
        School archived = Fixtures.school();
        archived.setId(2L);
        archived.setStatus(SchoolStatus.ARCHIVED);
        when(schoolRepository.findAll()).thenReturn(List.of(live, archived));
        when(departmentRepository.existsBySchoolIdAndNameIgnoreCase(any(), any())).thenReturn(true);
        service.seedDefaultsForExistingSchools();
        verify(departmentRepository, times(DepartmentService.DEFAULTS.size()))
                .existsBySchoolIdAndNameIgnoreCase(eq(1L), any());
        verify(departmentRepository, never()).existsBySchoolIdAndNameIgnoreCase(eq(2L), any());
    }
}
