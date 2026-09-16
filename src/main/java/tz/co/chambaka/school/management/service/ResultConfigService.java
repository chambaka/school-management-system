package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.academic.ResultMath;
import tz.co.chambaka.school.management.dto.academic.GradingBandRequest;
import tz.co.chambaka.school.management.dto.academic.GradingBandResponse;
import tz.co.chambaka.school.management.dto.academic.ResultWeightRequest;
import tz.co.chambaka.school.management.dto.academic.ResultWeightResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.GradingBand;
import tz.co.chambaka.school.management.model.ResultWeightConfig;
import tz.co.chambaka.school.management.model.Subject;
import tz.co.chambaka.school.management.repository.GradingBandRepository;
import tz.co.chambaka.school.management.repository.ResultWeightConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResultConfigService {

    private final ResultWeightConfigRepository weightRepository;
    private final GradingBandRepository gradingBandRepository;
    private final AcademicYearService academicYearService;
    private final AcademicTermService academicTermService;
    private final SubjectService subjectService;

    public ResultConfigService(
            ResultWeightConfigRepository weightRepository,
            GradingBandRepository gradingBandRepository,
            AcademicYearService academicYearService,
            AcademicTermService academicTermService,
            SubjectService subjectService
    ) {
        this.weightRepository = weightRepository;
        this.gradingBandRepository = gradingBandRepository;
        this.academicYearService = academicYearService;
        this.academicTermService = academicTermService;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<ResultWeightResponse> listWeights(Long schoolId, Long academicYearId) {
        return weightRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYearId).stream()
                .map(this::toWeight).toList();
    }

    @Transactional
    public ResultWeightResponse saveWeight(Long schoolId, ResultWeightRequest request) {
        if (request.midtermWeight().add(request.semiExamWeight()).compareTo(new BigDecimal("100")) != 0) {
            throw new BusinessException("Midterm and semi-terminal exam weights must add to 100");
        }
        if (request.semiResultWeight().add(request.terminalExamWeight()).compareTo(new BigDecimal("100")) != 0) {
            throw new BusinessException("Semi-terminal result and terminal exam weights must add to 100");
        }
        ResultWeightConfig config = weightRepository
                .findFirstBySchoolIdAndAcademicYearIdAndAcademicTermIdAndSubjectId(
                        schoolId, request.academicYearId(), request.academicTermId(), request.subjectId())
                .orElseGet(ResultWeightConfig::new);
        config.setSchoolId(schoolId);
        config.setAcademicYear(academicYearService.require(schoolId, request.academicYearId()));
        config.setAcademicTerm(request.academicTermId() == null ? null : academicTermService.require(schoolId, request.academicTermId()));
        config.setSubject(request.subjectId() == null ? null : subjectService.require(schoolId, request.subjectId()));
        config.setMidtermWeight(request.midtermWeight());
        config.setSemiExamWeight(request.semiExamWeight());
        config.setSemiResultWeight(request.semiResultWeight());
        config.setTerminalExamWeight(request.terminalExamWeight());
        return toWeight(weightRepository.save(config));
    }

    public ResultWeightConfig resolve(Long schoolId, Long academicYearId, Long termId, Long subjectId) {
        if (academicYearId != null) {
            var exact = weightRepository.findFirstBySchoolIdAndAcademicYearIdAndAcademicTermIdAndSubjectId(
                    schoolId, academicYearId, termId, subjectId);
            if (exact.isPresent()) {
                return exact.get();
            }
            var yearDefault = weightRepository.findFirstBySchoolIdAndAcademicYearIdAndAcademicTermIsNullAndSubjectIsNull(
                    schoolId, academicYearId);
            if (yearDefault.isPresent()) {
                return yearDefault.get();
            }
        }
        ResultWeightConfig fallback = new ResultWeightConfig();
        fallback.setMidtermWeight(ResultMath.DEFAULT_MIDTERM_WEIGHT);
        fallback.setSemiExamWeight(ResultMath.DEFAULT_SEMI_EXAM_WEIGHT);
        fallback.setSemiResultWeight(ResultMath.DEFAULT_SEMI_RESULT_WEIGHT);
        fallback.setTerminalExamWeight(ResultMath.DEFAULT_TERMINAL_EXAM_WEIGHT);
        return fallback;
    }

    @Transactional(readOnly = true)
    public List<GradingBandResponse> listBands(Long schoolId) {
        List<GradingBand> bands = gradingBandRepository.findBySchoolIdOrderBySortOrderAsc(schoolId);
        if (bands.isEmpty()) {
            return List.of(
                    new GradingBandResponse(null, 80, 100, "A", new BigDecimal("5.00"), 1),
                    new GradingBandResponse(null, 70, 79, "B", new BigDecimal("4.00"), 2),
                    new GradingBandResponse(null, 60, 69, "C", new BigDecimal("3.00"), 3),
                    new GradingBandResponse(null, 50, 59, "D", new BigDecimal("2.00"), 4),
                    new GradingBandResponse(null, 0, 49, "F", BigDecimal.ZERO, 5)
            );
        }
        return bands.stream().map(this::toBand).toList();
    }

    @Transactional
    public List<GradingBandResponse> replaceBands(Long schoolId, List<GradingBandRequest> requests) {
        gradingBandRepository.deleteAll(gradingBandRepository.findBySchoolIdOrderBySortOrderAsc(schoolId));
        int order = 0;
        for (GradingBandRequest request : requests) {
            GradingBand band = new GradingBand();
            band.setSchoolId(schoolId);
            band.setMinPercent(request.minPercent());
            band.setMaxPercent(request.maxPercent());
            band.setLetter(request.letter().trim().toUpperCase());
            band.setPoints(request.points());
            band.setSortOrder(request.sortOrder() == 0 ? ++order : request.sortOrder());
            gradingBandRepository.save(band);
        }
        return listBands(schoolId);
    }

    public String letterFor(Long schoolId, BigDecimal percentage) {
        List<ResultMath.GradeBand> bands = listBands(schoolId).stream()
                .map(b -> new ResultMath.GradeBand(b.minPercent(), b.maxPercent(), b.letter(), b.points()))
                .toList();
        return ResultMath.letter(percentage, bands);
    }

    private ResultWeightResponse toWeight(ResultWeightConfig config) {
        Subject subject = config.getSubject();
        return new ResultWeightResponse(
                config.getId(),
                config.getAcademicYear() != null ? config.getAcademicYear().getId() : null,
                config.getAcademicTerm() != null ? config.getAcademicTerm().getId() : null,
                subject != null ? subject.getId() : null,
                subject != null ? subject.getName() : null,
                config.getMidtermWeight(),
                config.getSemiExamWeight(),
                config.getSemiResultWeight(),
                config.getTerminalExamWeight()
        );
    }

    private GradingBandResponse toBand(GradingBand band) {
        return new GradingBandResponse(band.getId(), band.getMinPercent(), band.getMaxPercent(),
                band.getLetter(), band.getPoints(), band.getSortOrder());
    }
}
