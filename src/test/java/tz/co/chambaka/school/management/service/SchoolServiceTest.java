package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.school.UpdateSchoolRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.mapper.SchoolMapper;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.Tenant;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.model.enums.TenantStatus;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.TenantRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolServiceTest {

    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private SchoolMapper schoolMapper;
    @Mock
    private UserAccountService userAccountService;
    @InjectMocks
    private SchoolService schoolService;

    @Test
    void listGetAndBranding() {
        School school = Fixtures.school();
        when(schoolRepository.findAll()).thenReturn(List.of(school));
        when(schoolMapper.toResponse(school)).thenReturn(Fixtures.schoolResponse());
        when(tenantRepository.findAllById(List.of(10L))).thenReturn(List.of(Fixtures.tenant()));
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(Fixtures.tenant()));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.findBySlug("chambaka-secondary")).thenReturn(Optional.of(school));
        when(schoolRepository.findByCustomDomain("school.test")).thenReturn(Optional.of(school));
        when(schoolMapper.toBranding(school)).thenReturn(Fixtures.branding());

        assertThat(schoolService.list()).hasSize(1);
        assertThat(schoolService.list().getFirst().tenantName()).isEqualTo(Fixtures.tenant().getName());
        assertThat(schoolService.get(1L).id()).isEqualTo(1L);
        assertThat(schoolService.get(1L).tenantName()).isEqualTo(Fixtures.tenant().getName());
        assertThat(schoolService.brandingBySlug("chambaka-secondary").slug()).isEqualTo("chambaka");
        assertThat(schoolService.brandingByHost("school.test").id()).isEqualTo(1L);
    }

    @Test
    void getWithoutTenantLeavesOrganizationBlank() {
        School school = Fixtures.school();
        school.setTenantId(null);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolMapper.toResponse(school)).thenReturn(Fixtures.schoolResponse());
        assertThat(schoolService.get(1L).tenantName()).isNull();
    }

    @Test
    void listHandlesSchoolWithoutTenant() {
        School school = Fixtures.school();
        school.setTenantId(null);
        when(schoolRepository.findAll()).thenReturn(List.of(school));
        when(schoolMapper.toResponse(school)).thenReturn(Fixtures.schoolResponse());
        when(tenantRepository.findAllById(List.of())).thenReturn(List.of());
        assertThat(schoolService.list().getFirst().tenantName()).isNull();
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
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(Fixtures.tenant()));
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

    @Test
    void listHidesArchivedSchoolAndArchivedOrganization() {
        School live = Fixtures.school();
        School archivedSchool = Fixtures.school();
        archivedSchool.setId(2L);
        archivedSchool.setStatus(SchoolStatus.ARCHIVED);
        School ofArchivedOrg = Fixtures.school();
        ofArchivedOrg.setId(3L);
        ofArchivedOrg.setTenantId(11L);
        Tenant archivedTenant = Fixtures.tenant();
        archivedTenant.setId(11L);
        archivedTenant.setStatus(TenantStatus.ARCHIVED);
        when(schoolRepository.findAll()).thenReturn(List.of(live, archivedSchool, ofArchivedOrg));
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(Fixtures.tenant()));
        when(tenantRepository.findById(11L)).thenReturn(Optional.of(archivedTenant));
        when(schoolMapper.toResponse(live)).thenReturn(Fixtures.schoolResponse());
        when(tenantRepository.findAllById(List.of(10L))).thenReturn(List.of(Fixtures.tenant()));
        assertThat(schoolService.list()).hasSize(1);
    }

    @Test
    void brandingSkipsArchived() {
        School school = Fixtures.school();
        school.setStatus(SchoolStatus.ARCHIVED);
        when(schoolRepository.findBySlug("gone")).thenReturn(Optional.of(school));
        when(schoolRepository.findByCustomDomain("gone.test")).thenReturn(Optional.of(school));
        assertThatThrownBy(() -> schoolService.brandingBySlug("gone")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> schoolService.brandingByHost("gone.test")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireArchivedIsNotFound() {
        School school = Fixtures.school();
        school.setStatus(SchoolStatus.ARCHIVED);
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        assertThatThrownBy(() -> schoolService.require(1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRejectsArchiveAndMapsTrialToActive() {
        School school = Fixtures.school();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolMapper.toResponse(school)).thenReturn(Fixtures.schoolResponse());
        when(tenantRepository.findById(10L)).thenReturn(Optional.of(Fixtures.tenant()));
        assertThatThrownBy(() -> schoolService.update(1L, new UpdateSchoolRequest(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, SchoolStatus.ARCHIVED, null, null, null, null)))
                .isInstanceOf(BusinessException.class);
        schoolService.update(1L, new UpdateSchoolRequest(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, SchoolStatus.TRIAL, null, null, null, null));
        assertThat(school.getStatus()).isEqualTo(SchoolStatus.ACTIVE);
    }

    @Test
    void archiveDeletesSchoolUsers() {
        School school = Fixtures.school();
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        schoolService.archive(1L);
        assertThat(school.getStatus()).isEqualTo(SchoolStatus.ARCHIVED);
        verify(userAccountService).deleteForSchool(1L);
    }
}
