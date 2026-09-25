package tz.co.chambaka.school.management.curriculum;

import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.repository.LessonLogRepository;
import tz.co.chambaka.school.management.service.ClassService;
import tz.co.chambaka.school.management.service.SubjectService;
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
class CurriculumServiceTest {

    @Mock CurriculumTopicRepository topicRepository;
    @Mock SubjectService subjectService;
    @Mock ClassService classService;
    @Mock LessonLogRepository lessonLogRepository;
    @InjectMocks CurriculumService service;

    @Test
    void listsCreatesAndRequires() {
        CurriculumTopic topic = new CurriculumTopic();
        topic.setId(1L);
        topic.setSubject(Fixtures.subject());
        topic.setSchoolClass(Fixtures.schoolClass());
        topic.setTitle("Algebra");
        topic.setObjectives("Solve");
        topic.setSortOrder(1);
        when(topicRepository.findBySchoolIdOrderBySortOrderAscTitleAsc(1L)).thenReturn(List.of(topic));
        when(topicRepository.findBySchoolIdAndSubjectIdOrderBySortOrderAscTitleAsc(1L, 1L)).thenReturn(List.of(topic));
        assertThat(service.list(1L, null)).hasSize(1);
        assertThat(service.list(1L, 1L).getFirst().title()).isEqualTo("Algebra");

        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());
        when(classService.require(1L, 1L)).thenReturn(Fixtures.schoolClass());
        when(topicRepository.save(any(CurriculumTopic.class))).thenAnswer(inv -> {
            CurriculumTopic saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        assertThat(service.create(1L, new CurriculumTopicRequest(1L, 1L, "Geometry", "Draw", null)).id()).isEqualTo(2L);

        when(topicRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(topic));
        assertThat(service.require(1L, 1L).getTitle()).isEqualTo("Algebra");
        when(topicRepository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAndDelete() {
        CurriculumTopic topic = new CurriculumTopic();
        topic.setId(1L);
        topic.setSubject(Fixtures.subject());
        topic.setTitle("Algebra");
        when(topicRepository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(topic));
        when(subjectService.require(1L, 1L)).thenReturn(Fixtures.subject());

        assertThat(service.update(1L, 1L, new CurriculumTopicRequest(1L, null, "Geometry", "Draw shapes", 2)).title())
                .isEqualTo("Geometry");
        assertThat(topic.getObjectives()).isEqualTo("Draw shapes");
        assertThat(topic.getSchoolClass()).isNull();

        when(lessonLogRepository.existsByCurriculumTopicId(1L)).thenReturn(false);
        service.delete(1L, 1L);
        verify(topicRepository).delete(topic);

        when(lessonLogRepository.existsByCurriculumTopicId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class);
    }
}
