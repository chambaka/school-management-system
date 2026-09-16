package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AcademicTermRequest;
import tz.co.chambaka.school.management.dto.academic.AcademicTermResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AcademicTerm;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.repository.AcademicTermRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcademicTermService {

    private final AcademicTermRepository academicTermRepository;
    private final AcademicYearService academicYearService;

    public AcademicTermService(AcademicTermRepository academicTermRepository, AcademicYearService academicYearService) {
        this.academicTermRepository = academicTermRepository;
        this.academicYearService = academicYearService;
    }

    @Transactional(readOnly = true)
    public List<AcademicTermResponse> list(Long schoolId, Long academicYearId) {
        List<AcademicTerm> terms = academicYearId == null
                ? academicTermRepository.findBySchoolIdOrderByStartDateDesc(schoolId)
                : academicTermRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateAsc(schoolId, academicYearId);
        return terms.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AcademicTermResponse create(Long schoolId, AcademicTermRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("Term end date must be after start date");
        }
        AcademicYear year = academicYearService.require(schoolId, request.academicYearId());
        if (academicTermRepository.existsBySchoolIdAndAcademicYearIdAndNameIgnoreCase(
                schoolId, year.getId(), request.name())) {
            throw new DuplicateResourceException("Term already exists: " + request.name());
        }
        AcademicTerm term = new AcademicTerm();
        term.setSchoolId(schoolId);
        term.setAcademicYear(year);
        term.setName(request.name());
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
        term.setCurrentTerm(request.currentTerm());
        term = academicTermRepository.save(term);
        if (request.currentTerm()) {
            markCurrent(schoolId, year.getId(), term.getId());
        }
        return toResponse(term);
    }

    public AcademicTerm require(Long schoolId, Long id) {
        return academicTermRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("AcademicTerm", id));
    }

    private void markCurrent(Long schoolId, Long yearId, Long termId) {
        academicTermRepository.findBySchoolIdAndAcademicYearIdOrderByStartDateAsc(schoolId, yearId)
                .forEach(term -> term.setCurrentTerm(term.getId().equals(termId)));
    }

    private AcademicTermResponse toResponse(AcademicTerm term) {
        return new AcademicTermResponse(
                term.getId(),
                term.getAcademicYear().getId(),
                term.getAcademicYear().getName(),
                term.getName(),
                term.getStartDate(),
                term.getEndDate(),
                term.isCurrentTerm()
        );
    }
}
