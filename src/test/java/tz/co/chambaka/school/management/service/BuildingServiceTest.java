package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.BuildingRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Building;
import tz.co.chambaka.school.management.model.Classroom;
import tz.co.chambaka.school.management.repository.BuildingRepository;
import tz.co.chambaka.school.management.repository.ClassroomRepository;
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
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private ClassroomRepository classroomRepository;
    @InjectMocks
    private BuildingService service;

    @Test
    void listCreateUpdateAndDelete() {
        Building building = building("Block A");
        when(buildingRepository.findBySchoolIdOrderByNameAsc(1L)).thenReturn(List.of(building));
        assertThat(service.list(1L)).hasSize(1);

        when(buildingRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Block B")).thenReturn(false);
        when(buildingRepository.save(any(Building.class))).thenAnswer(inv -> {
            Building saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        assertThat(service.create(1L, new BuildingRequest(" Block B ", " Science ")).name()).isEqualTo("Block B");

        when(buildingRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(building));
        Classroom room = Fixtures.classroom();
        when(classroomRepository.findBySiteId(1L)).thenReturn(List.of(room));
        assertThat(service.update(1L, 1L, new BuildingRequest("Block C", null)).name()).isEqualTo("Block C");
        assertThat(room.getBuilding()).isEqualTo("Block C");

        when(classroomRepository.existsBySiteId(1L)).thenReturn(false);
        service.delete(1L, 1L);
        verify(buildingRepository).delete(building);
    }

    @Test
    void errors() {
        when(buildingRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Block A")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new BuildingRequest("Block A", null)))
                .isInstanceOf(DuplicateResourceException.class);

        Building building = building("Block A");
        when(buildingRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(building));
        when(buildingRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Hall")).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, 1L, new BuildingRequest("Hall", null)))
                .isInstanceOf(DuplicateResourceException.class);

        when(classroomRepository.existsBySiteId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class);

        when(buildingRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private static Building building(String name) {
        Building building = new Building();
        building.setId(1L);
        building.setSchoolId(1L);
        building.setName(name);
        return building;
    }
}
