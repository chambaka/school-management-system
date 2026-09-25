package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.dto.academic.AcademicTermRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.AcademicTerm;
import tz.co.chambaka.school.management.repository.AcademicTermRepository;
import tz.co.chambaka.school.management.repository.ExamRepository;
import tz.co.chambaka.school.management.repository.ResultWeightConfigRepository;
import tz.co.chambaka.school.management.support.Fixtures;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicTermServiceTest {

    @Mock AcademicTermRepository repository;
    @Mock AcademicYearService academicYearService;
    @Mock ExamRepository examRepository;
    @Mock ResultWeightConfigRepository resultWeightConfigRepository;
    @InjectMocks AcademicTermService service;

    @Test
    void listsAllOrByYearAndRequiresTerm() {
        AcademicTerm term = term(1L, "Term One", true);
        when(repository.findBySchoolIdOrderByStartDateDesc(1L)).thenReturn(List.of(term));
        when(repository.findBySchoolIdAndAcademicYearIdOrderByStartDateAsc(1L, 1L)).thenReturn(List.of(term));
        when(repository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(term));

        assertThat(service.list(1L, null)).singleElement().extracting("name").isEqualTo("Term One");
        assertThat(service.list(1L, 1L)).singleElement().extracting("academicYearName").isEqualTo("2026/2027");
        assertThat(service.require(1L, 1L)).isSameAs(term);
    }

    @Test
    void createsCurrentTermAndUnsetsOthers() {
        AcademicTerm old = term(1L, "Old", true);
        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(repository.existsBySchoolIdAndAcademicYearIdAndNameIgnoreCase(1L, 1L, "Term Two")).thenReturn(false);
        when(repository.save(any(AcademicTerm.class))).thenAnswer(invocation -> {
            AcademicTerm saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(repository.findBySchoolIdAndAcademicYearIdOrderByStartDateAsc(1L, 1L)).thenReturn(List.of(old));

        var response = service.create(1L, new AcademicTermRequest(
                1L, "Term Two", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 8, 1), true));

        assertThat(response.currentTerm()).isTrue();
        assertThat(old.isCurrentTerm()).isFalse();
    }

    @Test
    void rejectsInvalidDatesDuplicateAndMissingTerm() {
        assertThatThrownBy(() -> service.create(1L, new AcademicTermRequest(
                1L, "Bad", LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1), false)))
                .isInstanceOf(BusinessException.class);

        when(academicYearService.require(1L, 1L)).thenReturn(Fixtures.year());
        when(repository.existsBySchoolIdAndAcademicYearIdAndNameIgnoreCase(1L, 1L, "Term One")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, new AcademicTermRequest(
                1L, "Term One", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1), false)))
                .isInstanceOf(DuplicateResourceException.class);

        when(repository.findByIdAndSchoolId(9L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.require(1L, 9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesUnusedTerm() {
        AcademicTerm term = term(1L, "Term One", true);
        when(repository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(term));
        when(examRepository.existsByAcademicTermId(1L)).thenReturn(false);
        when(resultWeightConfigRepository.existsByAcademicTermId(1L)).thenReturn(false);

        service.delete(1L, 1L);

        verify(repository).delete(term);
    }

    @Test
    void deleteBlockedWhenTermIsInUse() {
        AcademicTerm term = term(1L, "Term One", true);
        when(repository.findByIdAndSchoolId(1L, 1L)).thenReturn(Optional.of(term));

        when(examRepository.existsByAcademicTermId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("exams");

        when(examRepository.existsByAcademicTermId(1L)).thenReturn(false);
        when(resultWeightConfigRepository.existsByAcademicTermId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("weights");
    }

    private AcademicTerm term(Long id, String name, boolean current) {
        AcademicTerm term = new AcademicTerm();
        term.setId(id);
        term.setSchoolId(1L);
        term.setAcademicYear(Fixtures.year());
        term.setName(name);
        term.setStartDate(LocalDate.of(2026, 1, 1));
        term.setEndDate(LocalDate.of(2026, 4, 1));
        term.setCurrentTerm(current);
        return term;
    }
}
