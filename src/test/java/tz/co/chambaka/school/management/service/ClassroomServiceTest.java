package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.ClassroomRequest;
import tz.co.chambaka.school.management.dto.academic.ClassroomResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.Classroom;
import tz.co.chambaka.school.management.model.TimetableSlot;
import tz.co.chambaka.school.management.repository.ClassroomRepository;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

    @Mock
    private ClassroomRepository classroomRepository;
    @Mock
    private TimetableSlotRepository timetableSlotRepository;
    @Mock
    private AcademicMapper academicMapper;
    @InjectMocks
    private ClassroomService service;

    @Test
    void listCreateAndUpdate() {
        Classroom classroom = Fixtures.classroom();
        ClassroomResponse dto = new ClassroomResponse(1L, "Lab 1", "L1", 40, "Block A", "Science");
        when(classroomRepository.findBySchoolIdOrderByNameAsc(1L)).thenReturn(List.of(classroom));
        when(academicMapper.toClassroom(any(Classroom.class))).thenReturn(dto);
        assertThat(service.list(1L)).hasSize(1);

        when(classroomRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Lab 1")).thenReturn(false);
        when(classroomRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "L1")).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenReturn(classroom);
        assertThat(service.create(1L, new ClassroomRequest(" Lab 1 ", " L1 ", 40, " Block A ", " Science ")).name())
                .isEqualTo("Lab 1");

        when(classroomRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(classroom));
        TimetableSlot renamed = slot("Lab 1");
        when(timetableSlotRepository.findBySchoolIdAndRoomIgnoreCase(1L, "Lab 1"))
                .thenReturn(List.of(renamed));
        service.update(1L, 1L, new ClassroomRequest("Lab 2", "L2", 30, "Block B", "Physics"));
        assertThat(classroom.getName()).isEqualTo("Lab 2");
        assertThat(renamed.getRoom()).isEqualTo("Lab 2");
        assertThat(classroom.getCode()).isEqualTo("L2");
        assertThat(classroom.getCapacity()).isEqualTo(30);
        assertThat(classroom.getBuilding()).isEqualTo("Block B");
        assertThat(classroom.getNotes()).isEqualTo("Physics");
    }

    @Test
    void createAllowsBlankOptionalFields() {
        Classroom classroom = Fixtures.classroom();
        when(classroomRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Hall")).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenReturn(classroom);
        when(academicMapper.toClassroom(classroom)).thenReturn(new ClassroomResponse(1L, "Hall", null, null, null, null));
        ClassroomResponse created = service.create(1L, new ClassroomRequest("Hall", "  ", null, "", null));
        assertThat(created.name()).isEqualTo("Hall");
        verify(classroomRepository, never()).existsBySchoolIdAndCodeIgnoreCase(any(), any());
    }

    @Test
    void updateKeepsSameNameAndCode() {
        Classroom classroom = Fixtures.classroom();
        when(classroomRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(classroom));
        when(academicMapper.toClassroom(classroom))
                .thenReturn(new ClassroomResponse(1L, "Lab 1", "l1", 40, "Block A", null));
        ClassroomResponse updated = service.update(1L, 1L, new ClassroomRequest("Lab 1", "l1", 40, "Block A", "  "));
        assertThat(updated.name()).isEqualTo("Lab 1");
        assertThat(classroom.getName()).isEqualTo("Lab 1");
        assertThat(classroom.getNotes()).isNull();
        verify(timetableSlotRepository, never()).findBySchoolIdAndRoomIgnoreCase(any(), any());
    }

    @Test
    void updateClearsCodeAndRenamesSlots() {
        Classroom classroom = Fixtures.classroom();
        classroom.setCode(null);
        TimetableSlot slot = slot("Lab 1");
        when(classroomRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(classroom));
        when(timetableSlotRepository.findBySchoolIdAndRoomIgnoreCase(1L, "Lab 1")).thenReturn(List.of(slot));
        when(academicMapper.toClassroom(classroom))
                .thenReturn(new ClassroomResponse(1L, "Hall", null, null, null, null));
        service.update(1L, 1L, new ClassroomRequest("Hall", null, null, null, null));
        assertThat(slot.getRoom()).isEqualTo("Hall");
        assertThat(classroom.getCode()).isNull();
    }

    @Test
    void deleteRemovesWhenUnused() {
        Classroom classroom = Fixtures.classroom();
        when(classroomRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(classroom));
        when(timetableSlotRepository.existsBySchoolIdAndRoomIgnoreCase(1L, "Lab 1")).thenReturn(false);
        service.delete(1L, 1L);
        verify(classroomRepository).delete(classroom);
    }

    @Test
    void errors() {
        when(classroomRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Lab 1")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new ClassroomRequest("Lab 1", null, null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);

        when(classroomRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Hall")).thenReturn(false);
        when(classroomRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "L1")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new ClassroomRequest("Hall", "L1", null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);

        Classroom existing = Fixtures.classroom();
        when(classroomRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(existing));
        when(classroomRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Hall")).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, 1L, new ClassroomRequest("Hall", "H1", null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);

        when(classroomRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Lab 3")).thenReturn(false);
        when(classroomRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "X1")).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, 1L, new ClassroomRequest("Lab 3", "X1", null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);

        existing.setCode(null);
        when(classroomRepository.existsBySchoolIdAndNameIgnoreCase(1L, "Lab 4")).thenReturn(false);
        when(classroomRepository.existsBySchoolIdAndCodeIgnoreCase(1L, "Z1")).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, 1L, new ClassroomRequest("Lab 4", "Z1", null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);

        when(timetableSlotRepository.existsBySchoolIdAndRoomIgnoreCase(1L, existing.getName())).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class);

        when(classroomRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private static TimetableSlot slot(String room) {
        TimetableSlot slot = new TimetableSlot();
        slot.setRoom(room);
        return slot;
    }
}
