package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.campus.CreateCampusRequest;
import tz.co.chambaka.school.management.dto.campus.UpdateCampusRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.CampusMapper;
import tz.co.chambaka.school.management.model.Campus;
import tz.co.chambaka.school.management.repository.CampusRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusServiceTest {

    @Mock
    private CampusRepository campusRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private CampusMapper campusMapper;
    @InjectMocks
    private CampusService campusService;

    @Test
    void listCreateUpdateAndDelete() {
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        when(campusRepository.findBySchoolIdOrderByPrimaryCampusDescNameAsc(1L))
                .thenReturn(List.of(Fixtures.campus()));
        when(campusMapper.toResponse(any(Campus.class))).thenReturn(Fixtures.campusResponse());
        when(campusRepository.findByTenantIdOrderBySchoolIdAscNameAsc(10L))
                .thenReturn(List.of(Fixtures.campus()));
        assertThat(campusService.listBySchool(1L)).hasSize(1);
        assertThat(campusService.listByTenant(10L)).hasSize(1);

        when(campusRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "EAST")).thenReturn(false);
        when(campusRepository.countBySchoolId(1L)).thenReturn(1L);
        when(campusRepository.save(any(Campus.class))).thenAnswer(inv -> {
            Campus campus = inv.getArgument(0);
            campus.setId(21L);
            return campus;
        });
        campusService.create(1L, new CreateCampusRequest("East", "east", "addr", "07", "e@e.com", "UTC", false));

        Campus existing = Fixtures.campus();
        existing.setPrimaryCampus(false);
        when(campusRepository.findByIdAndSchoolId(20L, 1L)).thenReturn(Optional.of(existing));
        when(campusRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "WEST")).thenReturn(false);
        when(campusRepository.countBySchoolId(1L)).thenReturn(2L);
        campusService.update(1L, 20L, new UpdateCampusRequest("West", "west", "a", "1", "w@e.com", "UTC", false));
        assertThat(existing.getName()).isEqualTo("West");
        assertThat(existing.isPrimaryCampus()).isFalse();

        campusService.delete(1L, 20L);
        verify(campusRepository).delete(existing);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        when(campusRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "MAIN")).thenReturn(true);
        assertThatThrownBy(() -> campusService.create(1L, new CreateCampusRequest("Main", "MAIN", null, null, null, null, false)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void createFirstCampusIsPrimary() {
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        when(campusRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "CAMPUS")).thenReturn(false);
        when(campusRepository.countBySchoolId(1L)).thenReturn(0L);
        when(campusRepository.save(any(Campus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campusMapper.toResponse(any(Campus.class))).thenReturn(Fixtures.campusResponse());
        campusService.create(1L, new CreateCampusRequest("Campus", null, null, null, null, null, false));
        verify(campusRepository).save(any(Campus.class));
    }

    @Test
    void updateRejectsDuplicateCode() {
        Campus existing = Fixtures.campus();
        when(campusRepository.findByIdAndSchoolId(20L, 1L)).thenReturn(Optional.of(existing));
        when(campusRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "EAST")).thenReturn(true);
        assertThatThrownBy(() -> campusService.update(1L, 20L, new UpdateCampusRequest(null, "east", null, null, null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void cannotDeletePrimaryOrLastCampus() {
        Campus primary = Fixtures.campus();
        when(campusRepository.findByIdAndSchoolId(20L, 1L)).thenReturn(Optional.of(primary));
        assertThatThrownBy(() -> campusService.delete(1L, 20L)).isInstanceOf(BusinessException.class);

        Campus other = Fixtures.campus();
        other.setPrimaryCampus(false);
        when(campusRepository.findByIdAndSchoolId(21L, 1L)).thenReturn(Optional.of(other));
        when(campusRepository.countBySchoolId(1L)).thenReturn(1L);
        assertThatThrownBy(() -> campusService.delete(1L, 21L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void requireHelpers() {
        when(campusRepository.findFirstBySchoolIdAndPrimaryCampusTrue(1L)).thenReturn(Optional.of(Fixtures.campus()));
        assertThat(campusService.requirePrimary(1L).getId()).isEqualTo(20L);
        when(campusRepository.findFirstBySchoolIdAndPrimaryCampusTrue(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> campusService.requirePrimary(2L)).isInstanceOf(ResourceNotFoundException.class);
        when(schoolRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> campusService.listBySchool(8L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPromotesPrimary() {
        Campus current = Fixtures.campus();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(Fixtures.school()));
        when(campusRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "NORTH")).thenReturn(false);
        when(campusRepository.findBySchoolIdOrderByPrimaryCampusDescNameAsc(1L))
                .thenReturn(new ArrayList<>(List.of(current)));
        when(campusRepository.save(any(Campus.class))).thenAnswer(inv -> inv.getArgument(0));
        when(campusMapper.toResponse(any(Campus.class))).thenReturn(Fixtures.campusResponse());
        campusService.create(1L, new CreateCampusRequest("North", "NORTH", null, null, null, null, true));
        assertThat(current.isPrimaryCampus()).isFalse();
    }
}
