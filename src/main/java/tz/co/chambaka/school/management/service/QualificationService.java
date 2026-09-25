package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.teacher.QualificationRequest;
import tz.co.chambaka.school.management.dto.teacher.QualificationResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Teacher;
import tz.co.chambaka.school.management.model.TeacherQualification;
import tz.co.chambaka.school.management.repository.TeacherQualificationRepository;
import tz.co.chambaka.school.management.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QualificationService {

    private final TeacherQualificationRepository qualificationRepository;
    private final TeacherRepository teacherRepository;

    public QualificationService(
            TeacherQualificationRepository qualificationRepository,
            TeacherRepository teacherRepository
    ) {
        this.qualificationRepository = qualificationRepository;
        this.teacherRepository = teacherRepository;
    }

    @Transactional(readOnly = true)
    public List<QualificationResponse> list() {
        return qualificationRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(QualificationService::toResponse)
                .toList();
    }

    @Transactional
    public QualificationResponse create(QualificationRequest request) {
        String name = request.name().trim();
        if (qualificationRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Qualification already exists");
        }
        TeacherQualification qualification = new TeacherQualification();
        qualification.setName(name);
        qualification.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        return toResponse(qualificationRepository.save(qualification));
    }

    @Transactional
    public QualificationResponse update(Long id, QualificationRequest request) {
        TeacherQualification qualification = require(id);
        String name = request.name().trim();
        if (!qualification.getName().equalsIgnoreCase(name)
                && qualificationRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Qualification already exists");
        }
        String previous = qualification.getName();
        qualification.setName(name);
        if (request.sortOrder() != null) {
            qualification.setSortOrder(request.sortOrder());
        }
        if (!previous.equals(name)) {
            for (Teacher teacher : teacherRepository.findByQualificationIgnoreCase(previous)) {
                teacher.setQualification(name);
            }
        }
        return toResponse(qualification);
    }

    @Transactional
    public void delete(Long id) {
        qualificationRepository.delete(require(id));
    }

    public TeacherQualification require(Long id) {
        return qualificationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Qualification", id));
    }

    public String requireByName(String name) {
        return qualificationRepository.findByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new BusinessException("Qualification is not configured"))
                .getName();
    }

    private static QualificationResponse toResponse(TeacherQualification qualification) {
        return new QualificationResponse(qualification.getId(), qualification.getName(), qualification.getSortOrder());
    }
}
