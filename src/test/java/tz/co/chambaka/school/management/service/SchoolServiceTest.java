package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.school.UpdateSchoolRequest;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.SchoolMapper;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolServiceTest {

    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private SchoolMapper schoolMapper;
    @InjectMocks
    private SchoolService schoolService;

    @Test
    void listGetAndBranding() {
        School school = Fixtures.school();
        when(schoolRepository.findAll()).thenReturn(List.of(school));
        when(schoolMapper.toResponse(school)).thenReturn(Fixtures.schoolResponse());
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.findBySlug("chambaka-secondary")).thenReturn(Optional.of(school));
        when(schoolRepository.findByCustomDomain("school.test")).thenReturn(Optional.of(school));
        when(schoolMapper.toBranding(school)).thenReturn(Fixtures.branding());

        assertThat(schoolService.list()).hasSize(1);
        assertThat(schoolService.get(1L).id()).isEqualTo(1L);
        assertThat(schoolService.brandingBySlug("chambaka-secondary").slug()).isEqualTo("chambaka");
        assertThat(schoolService.brandingByHost("school.test").id()).isEqualTo(1L);
    }

    @Test
    void brandingMissing() {
        when(schoolRepository.findBySlug("x")).thenReturn(Optional.empty());
        when(schoolRepository.findByCustomDomain("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> schoolService.brandingBySlug("x")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> schoolService.brandingByHost("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAllFieldsIncludingBlankDomain() {
        School school = Fixtures.school();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolMapper.toResponse(school)).thenReturn(Fixtures.schoolResponse());
        schoolService.update(1L, new UpdateSchoolRequest(
                "N", "e@e.com", "p", "addr", "web", "logo", "fav", "#1", "#2", "#3",
                "   ", "UTC", "sw", "USD", "KE", SchoolStatus.ACTIVE, "PRO",
                false, false, false));
        assertThat(school.getName()).isEqualTo("N");
        assertThat(school.getCustomDomain()).isNull();
        assertThat(school.isFinanceEnabled()).isFalse();
        assertThat(school.getSubscriptionPlan()).isEqualTo("PRO");
    }

    @Test
    void updateKeepsDomainWhenProvided() {
        School school = Fixtures.school();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolMapper.toResponse(school)).thenReturn(Fixtures.schoolResponse());
        schoolService.update(1L, new UpdateSchoolRequest(
                null, null, null, null, null, null, null, null, null, null,
                "app.school.tz", null, null, null, null, null, null, null, null, null));
        assertThat(school.getCustomDomain()).isEqualTo("app.school.tz");
    }

    @Test
    void requireMissing() {
        when(schoolRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> schoolService.require(8L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
