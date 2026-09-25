package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.AcademicYearRequest;
import tz.co.chambaka.school.management.dto.academic.AcademicYearResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.AcademicMapper;
import tz.co.chambaka.school.management.model.AcademicYear;
import tz.co.chambaka.school.management.repository.AcademicTermRepository;
import tz.co.chambaka.school.management.repository.AcademicYearRepository;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.FeeStructureRepository;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.ResultWeightConfigRepository;
import tz.co.chambaka.school.management.repository.SchoolClassRepository;
import tz.co.chambaka.school.management.repository.StudentEnrolmentRepository;
import tz.co.chambaka.school.management.repository.StudentRepository;
import tz.co.chambaka.school.management.repository.TeacherSubjectRepository;
import tz.co.chambaka.school.management.repository.TimetableLockRepository;
import tz.co.chambaka.school.management.repository.TimetableSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final AcademicMapper academicMapper;
    private final AcademicTermRepository academicTermRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final ExamRepository examRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final ResultWeightConfigRepository resultWeightConfigRepository;
    private final StudentEnrolmentRepository studentEnrolmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final TimetableLockRepository timetableLockRepository;

    public AcademicYearService(
            AcademicYearRepository academicYearRepository,
            AcademicMapper academicMapper,
            AcademicTermRepository academicTermRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository,
            ExamRepository examRepository,
            FeeStructureRepository feeStructureRepository,
            TeacherSubjectRepository teacherSubjectRepository,
            ResultWeightConfigRepository resultWeightConfigRepository,
            StudentEnrolmentRepository studentEnrolmentRepository,
            InvoiceRepository invoiceRepository,
            TimetableSlotRepository timetableSlotRepository,
            TimetableLockRepository timetableLockRepository
    ) {
        this.academicYearRepository = academicYearRepository;
        this.academicMapper = academicMapper;
        this.academicTermRepository = academicTermRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.examRepository = examRepository;
        this.feeStructureRepository = feeStructureRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.resultWeightConfigRepository = resultWeightConfigRepository;
        this.studentEnrolmentRepository = studentEnrolmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.timetableSlotRepository = timetableSlotRepository;
        this.timetableLockRepository = timetableLockRepository;
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

    @Transactional
    public void delete(Long schoolId, Long id) {
        AcademicYear year = require(schoolId, id);
        if (academicTermRepository.countByAcademicYearId(id) > 0) {
            throw new BusinessException("Delete terms in this year first");
        }
        if (schoolClassRepository.countByAcademicYearId(id) > 0) {
            throw new BusinessException("Delete classes in this year first");
        }
        if (studentRepository.countByAcademicYearId(id) > 0) {
            throw new BusinessException("Move or remove students from this year first");
        }
        if (examRepository.existsByAcademicYearId(id)) {
            throw new BusinessException("Remove exams for this year first");
        }
        if (feeStructureRepository.existsByAcademicYearId(id)) {
            throw new BusinessException("Remove fee structures for this year first");
        }
        if (teacherSubjectRepository.existsByAcademicYearId(id)) {
            throw new BusinessException("Remove teacher allocations for this year first");
        }
        if (resultWeightConfigRepository.existsByAcademicYearId(id)) {
            throw new BusinessException("Remove result weights for this year first");
        }
        if (studentEnrolmentRepository.existsByAcademicYearId(id)) {
            throw new BusinessException("This year has enrolment history and cannot be deleted");
        }
        if (invoiceRepository.existsByAcademicYearId(id)) {
            throw new BusinessException("Remove invoices for this year first");
        }
        if (timetableSlotRepository.existsByAcademicYearId(id)) {
            throw new BusinessException("Remove timetable slots for this year first");
        }
        if (timetableLockRepository.existsByAcademicYearId(id)) {
            throw new BusinessException("Unlock timetables for this year first");
        }
        academicYearRepository.delete(year);
    }

    public AcademicYear require(Long schoolId, Long id) {
        return academicYearRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("AcademicYear", id));
    }

    public AcademicYear nextAfter(Long schoolId, AcademicYear current) {
        List<AcademicYear> years = academicYearRepository.findBySchoolIdOrderByStartDateDesc(schoolId);
        if (years.isEmpty()) {
            return current;
        }
        if (current == null || current.getStartDate() == null) {
            return years.stream()
                    .filter(AcademicYear::isCurrentYear)
                    .findFirst()
                    .orElse(years.get(0));
        }
        return years.stream()
                .filter(year -> year.getStartDate() != null && year.getStartDate().isAfter(current.getStartDate()))
                .min(Comparator.comparing(AcademicYear::getStartDate))
                .or(() -> years.stream()
                        .filter(AcademicYear::isCurrentYear)
                        .filter(year -> !year.getId().equals(current.getId()))
                        .findFirst())
                .orElse(current);
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
