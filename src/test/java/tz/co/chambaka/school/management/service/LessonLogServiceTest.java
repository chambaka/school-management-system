package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.dto.academic.LessonLogRequest;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.LessonLog;
import tz.co.chambaka.school.management.model.TimetableSlot;
import tz.co.chambaka.school.management.repository.LessonLogRepository;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
import tz.co.chambaka.school.management.support.Fixtures;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonLogServiceTest {

    @Mock LessonLogRepository repository;
    @Mock TeacherService teacherService;
    @Mock SectionService sectionService;
    @Mock SubjectService subjectService;
    @Mock TimetableSlotRepository timetableSlotRepository;
    @InjectMocks LessonLogService service;

    @Test
    void createsWithAndWithoutTimetableSlot() {
        stubRelations();
        TimetableSlot slot = new TimetableSlot();
        slot.setId(5L);
        when(timetableSlotRepository.findByIdAndSchoolId(5L, 1L)).thenReturn(Optional.of(slot));
        when(repository.save(any(LessonLog.class))).thenAnswer(invocation -> {
            LessonLog saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        assertThat(service.create(1L, 3L, request(5L)).timetableSlotId()).isEqualTo(5L);
        assertThat(service.create(1L, 3L, request(null)).timetableSlotId()).isNull();
    }

    @Test
    void rejectsMissingSlotAndListsBySection() {
        stubRelations();
        when(timetableSlotRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(1L, 3L, request(9L)))
                .isInstanceOf(ResourceNotFoundException.class);

        LessonLog log = log();
        when(repository.findBySchoolIdAndSectionIdAndLessonDate(1L, 1L, LocalDate.of(2026, 9, 16)))
                .thenReturn(List.of(log));
        assertThat(service.bySection(1L, 1L, LocalDate.of(2026, 9, 16)))
                .singleElement().satisfies(row -> assertThat(row.topic()).isEqualTo("Fractions"));
    }

    private void stubRelations() {
        when(teacherService.requireByUser(3L)).thenReturn(Fixtures.teacher());
        when(sectionService.require(1L, 1L)).thenReturn(Fixtures.section());
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
    }

    private LessonLogRequest request(Long slotId) {
        return new LessonLogRequest(slotId, 1L, 1L, LocalDate.of(2026, 9, 16),
                "Fractions", "Add fractions", "Board", "Exercise 1");
    }

    private LessonLog log() {
        LessonLog log = new LessonLog();
        log.setId(1L);
        log.setTeacher(Fixtures.teacher());
        log.setSection(Fixtures.section());
        log.setSubject(Fixtures.subject());
        log.setLessonDate(LocalDate.of(2026, 9, 16));
        log.setTopic("Fractions");
        log.setObjectives("Add fractions");
        log.setMaterials("Board");
        log.setHomework("Exercise 1");
        return log;
    }
}
