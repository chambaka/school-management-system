package tz.co.chambaka.school.management.curriculum;

import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.service.ClassService;
import tz.co.chambaka.school.management.service.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CurriculumService {

    private final CurriculumTopicRepository topicRepository;
    private final SubjectService subjectService;
    private final ClassService classService;

    public CurriculumService(
            CurriculumTopicRepository topicRepository,
            SubjectService subjectService,
            ClassService classService
    ) {
        this.topicRepository = topicRepository;
        this.subjectService = subjectService;
        this.classService = classService;
    }

    @Transactional(readOnly = true)
    public List<CurriculumTopicResponse> list(Long schoolId, Long subjectId) {
        List<CurriculumTopic> topics = subjectId == null
                ? topicRepository.findBySchoolIdOrderBySortOrderAscTitleAsc(schoolId)
                : topicRepository.findBySchoolIdAndSubjectIdOrderBySortOrderAscTitleAsc(schoolId, subjectId);
        return topics.stream().map(CurriculumService::toResponse).toList();
    }

    @Transactional
    public CurriculumTopicResponse create(Long schoolId, CurriculumTopicRequest request) {
        CurriculumTopic topic = new CurriculumTopic();
        topic.setSchoolId(schoolId);
        topic.setSubject(subjectService.require(schoolId, request.subjectId()));
        topic.setSchoolClass(request.schoolClassId() == null ? null : classService.require(schoolId, request.schoolClassId()));
        topic.setTitle(request.title());
        topic.setObjectives(request.objectives());
        topic.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        return toResponse(topicRepository.save(topic));
    }

    public CurriculumTopic require(Long schoolId, Long id) {
        return topicRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Curriculum topic", id));
    }

    private static CurriculumTopicResponse toResponse(CurriculumTopic topic) {
        return new CurriculumTopicResponse(
                topic.getId(),
                topic.getSubject().getId(),
                topic.getSubject().getName(),
                topic.getSchoolClass() == null ? null : topic.getSchoolClass().getId(),
                topic.getSchoolClass() == null ? null : topic.getSchoolClass().getName(),
                topic.getTitle(),
                topic.getObjectives(),
                topic.getSortOrder()
        );
    }
}
