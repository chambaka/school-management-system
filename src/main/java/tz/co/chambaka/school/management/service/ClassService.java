package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.SchoolClassRequest;
import tz.co.chambaka.school.management.dto.academic.SchoolClassResponse;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.model.SchoolClass;
import tz.co.chambaka.school.management.repository.SchoolClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final AcademicYearService academicYearService;

    public ClassService(SchoolClassRepository schoolClassRepository, AcademicYearService academicYearService) {
        this.schoolClassRepository = schoolClassRepository;
        this.academicYearService = academicYearService;
    }

    @Transactional(readOnly = true)
    public List<SchoolClassResponse> list(Long schoolId, Long academicYearId) {
        List<SchoolClass> classes = academicYearId == null
                ? schoolClassRepository.findBySchoolIdOrderByDisplayOrderAsc(schoolId)
                : schoolClassRepository.findBySchoolIdAndAcademicYearIdOrderByDisplayOrderAsc(schoolId, academicYearId);
        return classes.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SchoolClassResponse create(Long schoolId, SchoolClassRequest request) {
        AcademicYear year = academicYearService.require(schoolId, request.academicYearId());
        if (schoolClassRepository.existsBySchoolIdAndAcademicYearIdAndCodeIgnoreCase(
                schoolId, year.getId(), request.code())) {
            throw new DuplicateResourceException("Class code already exists in this academic year");
        }
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setSchoolId(schoolId);
        schoolClass.setAcademicYear(year);
        schoolClass.setName(request.name());
        schoolClass.setCode(request.code());
        schoolClass.setDisplayOrder(request.displayOrder());
        return toResponse(schoolClassRepository.save(schoolClass));
    }

    @Transactional
    public SchoolClassResponse update(Long schoolId, Long id, SchoolClassRequest request) {
        SchoolClass schoolClass = require(schoolId, id);
        AcademicYear year = academicYearService.require(schoolId, request.academicYearId());
        schoolClass.setAcademicYear(year);
        schoolClass.setName(request.name());
        schoolClass.setCode(request.code());
        schoolClass.setDisplayOrder(request.displayOrder());
        return toResponse(schoolClass);
    }

    public SchoolClass require(Long schoolId, Long id) {
        return schoolClassRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("SchoolClass", id));
    }

    private SchoolClassResponse toResponse(SchoolClass schoolClass) {
        return new SchoolClassResponse(
                schoolClass.getId(),
                schoolClass.getAcademicYear().getId(),
                schoolClass.getAcademicYear().getName(),
                schoolClass.getName(),
                schoolClass.getCode(),
                schoolClass.getDisplayOrder()
        );
    }
}
