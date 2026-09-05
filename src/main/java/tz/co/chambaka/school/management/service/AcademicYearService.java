package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AcademicYearRequest;
import tz.co.chambaka.school.management.dto.academic.AcademicYearResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.repository.AcademicYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final AcademicMapper academicMapper;

    public AcademicYearService(AcademicYearRepository academicYearRepository, AcademicMapper academicMapper) {
        this.academicYearRepository = academicYearRepository;
        this.academicMapper = academicMapper;
    }

    @Transactional(readOnly = true)
    public List<AcademicYearResponse> list(Long schoolId) {
        return academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId)
                .stream().map(academicMapper::toYear).toList();
    }

    @Transactional
    public AcademicYearResponse create(Long schoolId, AcademicYearRequest request) {
        validateDates(request);
        if (academicYearRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, request.name())) {
            throw new DuplicateResourceException("Academic year already exists: " + request.name());
        }
        AcademicYear year = new AcademicYear();
        year.setSchoolId(schoolId);
        year.setName(request.name());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setCurrentYear(request.currentYear());
        year = academicYearRepository.save(year);
        if (request.currentYear()) {
            markCurrent(schoolId, year.getId());
        }
        return academicMapper.toYear(year);
    }

    @Transactional
    public AcademicYearResponse update(Long schoolId, Long id, AcademicYearRequest request) {
        validateDates(request);
        AcademicYear year = require(schoolId, id);
        year.setName(request.name());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setCurrentYear(request.currentYear());
        if (request.currentYear()) {
            markCurrent(schoolId, year.getId());
        }
        return academicMapper.toYear(year);
    }

    @Transactional
    public void setCurrent(Long schoolId, Long id) {
        require(schoolId, id);
        markCurrent(schoolId, id);
    }

    public AcademicYear require(Long schoolId, Long id) {
        return academicYearRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("AcademicYear", id));
    }

    private void markCurrent(Long schoolId, Long id) {
        academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId).forEach(year ->
                year.setCurrentYear(year.getId().equals(id)));
    }

    private void validateDates(AcademicYearRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("End date must be after start date");
        }
    }
}
