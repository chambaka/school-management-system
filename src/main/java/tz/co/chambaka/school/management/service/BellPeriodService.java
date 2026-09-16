package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.BellPeriodRequest;
import tz.co.chambaka.school.management.dto.academic.BellPeriodResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.BellPeriod;
import tz.co.chambaka.school.management.model.enums.PeriodKind;
import tz.co.chambaka.school.management.repository.BellPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class BellPeriodService {

    private final BellPeriodRepository bellPeriodRepository;

    public BellPeriodService(BellPeriodRepository bellPeriodRepository) {
        this.bellPeriodRepository = bellPeriodRepository;
    }

    @Transactional(readOnly = true)
    public List<BellPeriodResponse> list(Long schoolId) {
        List<BellPeriod> periods = bellPeriodRepository.findBySchoolIdOrderBySortOrderAsc(schoolId);
        if (periods.isEmpty()) {
            return defaults();
        }
        return periods.stream().map(this::toResponse).toList();
    }

    @Transactional
    public BellPeriodResponse create(Long schoolId, BellPeriodRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException("Period end must be after start");
        }
        BellPeriod period = new BellPeriod();
        period.setSchoolId(schoolId);
        period.setName(request.name());
        period.setStartTime(request.startTime());
        period.setEndTime(request.endTime());
        period.setKind(request.kind());
        period.setSortOrder(request.sortOrder());
        return toResponse(bellPeriodRepository.save(period));
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        BellPeriod period = bellPeriodRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("BellPeriod", id));
        bellPeriodRepository.delete(period);
    }

    public List<BellPeriod> lessonPeriods(Long schoolId) {
        List<BellPeriod> stored = bellPeriodRepository.findBySchoolIdOrderBySortOrderAsc(schoolId);
        if (stored.isEmpty()) {
            return defaults().stream().filter(p -> p.kind() == PeriodKind.LESSON).map(p -> {
                BellPeriod period = new BellPeriod();
                period.setName(p.name());
                period.setStartTime(p.startTime());
                period.setEndTime(p.endTime());
                period.setKind(PeriodKind.LESSON);
                period.setSortOrder(p.sortOrder());
                return period;
            }).toList();
        }
        return stored.stream().filter(p -> p.getKind() == PeriodKind.LESSON).toList();
    }

    private List<BellPeriodResponse> defaults() {
        return List.of(
                new BellPeriodResponse(null, "P1", LocalTime.of(7, 30), LocalTime.of(8, 10), PeriodKind.LESSON, 1),
                new BellPeriodResponse(null, "P2", LocalTime.of(8, 10), LocalTime.of(8, 50), PeriodKind.LESSON, 2),
                new BellPeriodResponse(null, "P3", LocalTime.of(8, 50), LocalTime.of(9, 30), PeriodKind.LESSON, 3),
                new BellPeriodResponse(null, "Short break", LocalTime.of(9, 30), LocalTime.of(9, 50), PeriodKind.BREAK, 4),
                new BellPeriodResponse(null, "P4", LocalTime.of(9, 50), LocalTime.of(10, 30), PeriodKind.LESSON, 5),
                new BellPeriodResponse(null, "P5", LocalTime.of(10, 30), LocalTime.of(11, 10), PeriodKind.LESSON, 6),
                new BellPeriodResponse(null, "P6", LocalTime.of(11, 10), LocalTime.of(11, 50), PeriodKind.LESSON, 7),
                new BellPeriodResponse(null, "Lunch", LocalTime.of(11, 50), LocalTime.of(12, 30), PeriodKind.BREAK, 8),
                new BellPeriodResponse(null, "P7", LocalTime.of(12, 30), LocalTime.of(13, 10), PeriodKind.LESSON, 9),
                new BellPeriodResponse(null, "P8", LocalTime.of(13, 10), LocalTime.of(13, 50), PeriodKind.LESSON, 10)
        );
    }

    private BellPeriodResponse toResponse(BellPeriod period) {
        return new BellPeriodResponse(period.getId(), period.getName(), period.getStartTime(), period.getEndTime(),
                period.getKind(), period.getSortOrder());
    }
}
