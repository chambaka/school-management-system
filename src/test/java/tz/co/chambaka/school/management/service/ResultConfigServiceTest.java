package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.dto.academic.GradingBandRequest;
import tz.co.chambaka.school.management.dto.academic.ResultWeightRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.AcademicTerm;
import tz.co.chambaka.school.management.model.GradingBand;
import tz.co.chambaka.school.management.model.ResultWeightConfig;
import tz.co.chambaka.school.management.repository.GradingBandRepository;
import tz.co.chambaka.school.management.repository.ResultWeightConfigRepository;
import tz.co.chambaka.school.management.support.Fixtures;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultConfigServiceTest {

    @Mock ResultWeightConfigRepository weightRepository;
    @Mock GradingBandRepository gradingBandRepository;
    @Mock AcademicYearService academicYearService;
    @Mock AcademicTermService academicTermService;
    @Mock SubjectService subjectService;
    @InjectMocks ResultConfigService service;

    @Test
    void savesGlobalAndSpecificWeights() {
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(weightRepository.save(any(ResultWeightConfig.class))).thenAnswer(invocation -> {
            ResultWeightConfig config = invocation.getArgument(0);
            config.setId(1L);
            return config;
        });
        ResultWeightRequest global = request(null, null);
        assertThat(service.saveWeight(1L, global).subjectId()).isNull();

        AcademicTerm term = new AcademicTerm();
        term.setId(2L);
        when(academicTermService.require(1L, 2L)).thenReturn(term);
        when(subjectService.require(1L, 3L)).thenReturn(Fixtures.subject());
        ResultWeightConfig existing = new ResultWeightConfig();
        when(weightRepository.findFirstBySchoolIdAndAcademicYearIdAndAcademicTermIdAndSubjectId(1L, 1L, 2L, 3L))
                .thenReturn(Optional.of(existing));
        var response = service.saveWeight(1L, request(2L, 3L));
        assertThat(response.academicTermId()).isEqualTo(2L);
        assertThat(response.subjectName()).isEqualTo("Mathematics");
    }

    @Test
    void validatesBothWeightPairs() {
        assertThatThrownBy(() -> service.saveWeight(1L, new ResultWeightRequest(
                1L, null, null, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.valueOf(50), BigDecimal.valueOf(50))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.saveWeight(1L, new ResultWeightRequest(
                1L, null, null, BigDecimal.TEN, BigDecimal.valueOf(90), BigDecimal.ONE, BigDecimal.ONE)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void resolvesExactYearDefaultAndFallback() {
        ResultWeightConfig exact = config(20, 80, 40, 60);
        when(weightRepository.findFirstBySchoolIdAndAcademicYearIdAndAcademicTermIdAndSubjectId(1L, 1L, 2L, 3L))
                .thenReturn(Optional.of(exact));
        assertThat(service.resolve(1L, 1L, 2L, 3L)).isSameAs(exact);

        ResultWeightConfig yearDefault = config(10, 90, 50, 50);
        when(weightRepository.findFirstBySchoolIdAndAcademicYearIdAndAcademicTermIdAndSubjectId(1L, 2L, 2L, 3L))
                .thenReturn(Optional.empty());
        when(weightRepository.findFirstBySchoolIdAndAcademicYearIdAndAcademicTermIsNullAndSubjectIsNull(1L, 2L))
                .thenReturn(Optional.of(yearDefault));
        assertThat(service.resolve(1L, 2L, 2L, 3L)).isSameAs(yearDefault);
        assertThat(service.resolve(1L, null, null, null).getMidtermWeight()).isEqualByComparingTo("10");
    }

    @Test
    void listsDefaultAndStoredBandsAndReplacesConfiguration() {
        when(gradingBandRepository.findBySchoolIdOrderBySortOrderAsc(1L)).thenReturn(List.of());
        assertThat(service.listBands(1L)).hasSize(5);
        assertThat(service.letterFor(1L, BigDecimal.valueOf(81))).isEqualTo("A");

        GradingBand band = band(" b ", 70, 79, 4, 2);
        when(gradingBandRepository.findBySchoolIdOrderBySortOrderAsc(2L)).thenReturn(List.of(band));
        assertThat(service.listBands(2L)).singleElement().satisfies(row -> {
            assertThat(row.letter()).isEqualTo(" b ");
            assertThat(row.points()).isEqualByComparingTo("4");
        });

        service.replaceBands(1L, List.of(
                new GradingBandRequest(80, 100, " a ", BigDecimal.valueOf(5), 0),
                new GradingBandRequest(0, 79, "f", BigDecimal.ZERO, 8)));
        verify(gradingBandRepository).deleteAll(any());
        verify(gradingBandRepository, times(2)).save(any(GradingBand.class));
    }

    @Test
    void listsSavedWeights() {
        ResultWeightConfig config = config(10, 90, 50, 50);
        config.setId(1L);
        config.setAcademicYear(Fixtures.year());
        config.setSubject(Fixtures.subject());
        when(weightRepository.findBySchoolIdAndAcademicYearId(1L, 1L)).thenReturn(List.of(config));
        assertThat(service.listWeights(1L, 1L)).singleElement()
                .satisfies(row -> assertThat(row.subjectName()).isEqualTo("Mathematics"));
    }

    private ResultWeightRequest request(Long termId, Long subjectId) {
        return new ResultWeightRequest(1L, termId, subjectId,
                BigDecimal.TEN, BigDecimal.valueOf(90), BigDecimal.valueOf(50), BigDecimal.valueOf(50));
    }

    private ResultWeightConfig config(int mid, int semiExam, int semi, int terminal) {
        ResultWeightConfig config = new ResultWeightConfig();
        config.setMidtermWeight(BigDecimal.valueOf(mid));
        config.setSemiExamWeight(BigDecimal.valueOf(semiExam));
        config.setSemiResultWeight(BigDecimal.valueOf(semi));
        config.setTerminalExamWeight(BigDecimal.valueOf(terminal));
        return config;
    }

    private GradingBand band(String letter, int min, int max, int points, int order) {
        GradingBand band = new GradingBand();
        band.setId(1L);
        band.setMinPercent(min);
        band.setMaxPercent(max);
        band.setLetter(letter);
        band.setPoints(BigDecimal.valueOf(points));
        band.setSortOrder(order);
        return band;
    }
}
