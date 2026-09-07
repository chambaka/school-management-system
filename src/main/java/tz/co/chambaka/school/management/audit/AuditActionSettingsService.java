package tz.co.chambaka.school.management.audit;

import tz.co.chambaka.school.management.dto.audit.AuditActionSettingResponse;
import tz.co.chambaka.school.management.dto.audit.AuditActionSettingsResponse;
import tz.co.chambaka.school.management.dto.audit.ReplaceAuditActionSettingsRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.model.AuditActionSetting;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.repository.AuditActionSettingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuditActionSettingsService {

    private static final Logger log = LoggerFactory.getLogger(AuditActionSettingsService.class);

    private final AuditActionSettingRepository repository;
    private final Map<AuditAction, Boolean> enabledByAction = new ConcurrentHashMap<>();

    public AuditActionSettingsService(AuditActionSettingRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void ensureDefaults() {
        for (AuditAction action : AuditAction.values()) {
            if (!repository.existsById(action)) {
                AuditActionSetting setting = new AuditActionSetting();
                setting.setAction(action);
                setting.setEnabled(true);
                repository.save(setting);
            }
        }
        reload();
        log.info("Loaded {} audit action settings", enabledByAction.size());
    }

    public boolean isEnabled(AuditAction action) {
        if (action == null) {
            return false;
        }
        Boolean enabled = enabledByAction.get(action);
        return enabled == null || enabled;
    }

    @Transactional(readOnly = true)
    public AuditActionSettingsResponse list() {
        if (enabledByAction.isEmpty()) {
            reload();
        }
        List<AuditActionSettingResponse> events = AuditActionCatalog.all().stream()
                .sorted(Comparator.comparing(AuditActionCatalog.Meta::group)
                        .thenComparing(meta -> meta.action().name()))
                .map(this::toResponse)
                .toList();
        return new AuditActionSettingsResponse(events);
    }

    @Transactional
    public AuditActionSettingResponse update(AuditAction action, boolean enabled) {
        persist(action, enabled);
        reload();
        return toResponse(AuditActionCatalog.meta(action));
    }

    @Transactional
    public AuditActionSettingsResponse replaceAll(ReplaceAuditActionSettingsRequest request) {
        if (request == null || request.events() == null || request.events().isEmpty()) {
            throw new BusinessException("At least one audit event is required");
        }
        for (ReplaceAuditActionSettingsRequest.Item item : request.events()) {
            if (item == null || item.action() == null || item.enabled() == null) {
                throw new BusinessException("Each event needs an action and enabled flag");
            }
            persist(item.action(), item.enabled());
        }
        reload();
        return list();
    }

    private void persist(AuditAction action, boolean enabled) {
        AuditActionSetting setting = repository.findById(action).orElseGet(AuditActionSetting::new);
        setting.setAction(action);
        setting.setEnabled(enabled);
        repository.save(setting);
        log.info("Audit action {} set enabled={}", action, enabled);
    }

    private void reload() {
        Map<AuditAction, Boolean> next = new EnumMap<>(AuditAction.class);
        for (AuditAction action : AuditAction.values()) {
            next.put(action, true);
        }
        for (AuditActionSetting setting : repository.findAll()) {
            if (setting.getAction() != null) {
                next.put(setting.getAction(), setting.isEnabled());
            }
        }
        enabledByAction.clear();
        enabledByAction.putAll(next);
    }

    private AuditActionSettingResponse toResponse(AuditActionCatalog.Meta meta) {
        return new AuditActionSettingResponse(
                meta.action(),
                meta.group(),
                meta.label(),
                meta.description(),
                isEnabled(meta.action()));
    }
}
