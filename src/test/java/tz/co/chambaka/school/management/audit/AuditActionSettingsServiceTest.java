package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.dto.audit.ReplaceAuditActionSettingsRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.AuditActionSetting;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.repository.AuditActionSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditActionSettingsServiceTest {

    @Mock
    private AuditActionSettingRepository repository;
    @InjectMocks
    private AuditActionSettingsService service;

    @Test
    void catalogCoversEveryAction() {
        assertThat(AuditActionCatalog.all()).hasSize(AuditAction.values().length);
        for (AuditAction action : AuditAction.values()) {
            AuditActionCatalog.Meta meta = AuditActionCatalog.meta(action);
            assertThat(meta.action()).isEqualTo(action);
            assertThat(meta.group()).isNotBlank();
            assertThat(meta.label()).isNotBlank();
            assertThat(meta.description()).isNotBlank();
        }
        assertThat(AuditActionCatalog.meta(AuditAction.PAYMENT_RECORDED).label()).isEqualTo("Payment posted");
    }

    @Test
    void ensureDefaultsInsertsMissingAndTreatsUnknownAsEnabled() {
        when(repository.existsById(any())).thenAnswer(inv -> inv.getArgument(0) == AuditAction.LOGIN);
        when(repository.findAll()).thenReturn(List.of());
        service.ensureDefaults();
        verify(repository, times(AuditAction.values().length - 1)).save(any(AuditActionSetting.class));
        assertThat(service.isEnabled(AuditAction.LOGIN)).isTrue();
        assertThat(service.isEnabled(null)).isFalse();
    }

    @Test
    void listReloadsCacheAndSkipsNullActions() {
        AuditActionSetting disabled = new AuditActionSetting();
        disabled.setAction(AuditAction.LOGIN);
        disabled.setEnabled(false);
        AuditActionSetting orphan = new AuditActionSetting();
        when(repository.findAll()).thenReturn(List.of(disabled, orphan));
        var listed = service.list();
        assertThat(listed.events()).hasSize(AuditAction.values().length);
        assertThat(service.list().events()).hasSize(AuditAction.values().length);
        verify(repository, times(1)).findAll();
        assertThat(service.isEnabled(AuditAction.LOGIN)).isFalse();
        assertThat(service.isEnabled(AuditAction.PAYMENT_RECORDED)).isTrue();
        assertThat(listed.events().stream().anyMatch(e -> e.action() == AuditAction.LOGIN && !e.enabled())).isTrue();
    }

    @Test
    void updatePersistsExistingAndNewRows() {
        AuditActionSetting existing = new AuditActionSetting();
        existing.setAction(AuditAction.CREATE);
        existing.setEnabled(true);
        when(repository.findById(AuditAction.CREATE)).thenReturn(Optional.of(existing));
        when(repository.findById(AuditAction.ERROR)).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(setting(AuditAction.CREATE, false), setting(AuditAction.ERROR, false)));
        assertThat(service.update(AuditAction.CREATE, false).enabled()).isFalse();
        assertThat(service.update(AuditAction.ERROR, false).action()).isEqualTo(AuditAction.ERROR);
        verify(repository, times(2)).save(any(AuditActionSetting.class));
    }

    @Test
    void replaceAllValidatesAndApplies() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(setting(AuditAction.ACCESS, false)));
        var result = service.replaceAll(new ReplaceAuditActionSettingsRequest(List.of(
                new ReplaceAuditActionSettingsRequest.Item(AuditAction.ACCESS, false))));
        assertThat(result.events()).isNotEmpty();
        assertThatThrownBy(() -> service.replaceAll(null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.replaceAll(new ReplaceAuditActionSettingsRequest(List.of())))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.replaceAll(new ReplaceAuditActionSettingsRequest(null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.replaceAll(new ReplaceAuditActionSettingsRequest(java.util.Arrays.asList(
                (ReplaceAuditActionSettingsRequest.Item) null))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.replaceAll(new ReplaceAuditActionSettingsRequest(List.of(
                new ReplaceAuditActionSettingsRequest.Item(null, true)))))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.replaceAll(new ReplaceAuditActionSettingsRequest(List.of(
                new ReplaceAuditActionSettingsRequest.Item(AuditAction.LOGIN, null)))))
                .isInstanceOf(BusinessException.class);
        verify(repository, times(1)).save(any());
        ArgumentCaptor<AuditActionSetting> captor = ArgumentCaptor.forClass(AuditActionSetting.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isFalse();
        verify(repository, never()).delete(any());
    }

    private static AuditActionSetting setting(AuditAction action, boolean enabled) {
        AuditActionSetting setting = new AuditActionSetting();
        setting.setAction(action);
        setting.setEnabled(enabled);
        return setting;
    }
}
