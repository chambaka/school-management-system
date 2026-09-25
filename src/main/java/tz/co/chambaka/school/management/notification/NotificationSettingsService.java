package tz.co.chambaka.school.management.notification;

import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationSettingsService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationSettingsService(
            NotificationTemplateRepository templateRepository,
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository
    ) {
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> templates(Long schoolId) {
        return templateRepository.findBySchoolIdOrderByEventKeyAsc(schoolId).stream()
                .map(NotificationSettingsService::toTemplate)
                .toList();
    }

    @Transactional
    public NotificationTemplateResponse saveTemplate(Long schoolId, NotificationTemplateRequest request) {
        NotificationChannel channel = request.channel() == null ? NotificationChannel.IN_APP : request.channel();
        NotificationTemplate template = templateRepository
                .findBySchoolIdAndEventKeyAndChannel(schoolId, request.eventKey(), channel)
                .orElseGet(NotificationTemplate::new);
        template.setSchoolId(schoolId);
        template.setEventKey(request.eventKey());
        template.setChannel(channel);
        template.setSubject(request.subject());
        template.setBody(request.body());
        return toTemplate(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> preferences(Long userId) {
        return preferenceRepository.findByUserId(userId).stream()
                .map(pref -> new NotificationPreferenceResponse(
                        pref.getEventKey(), pref.isInApp(), pref.isEmail(), pref.isSms(), pref.isPush()))
                .toList();
    }

    @Transactional
    public NotificationPreferenceResponse savePreference(Long userId, NotificationPreferenceRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        NotificationPreference pref = preferenceRepository.findByUserIdAndEventKey(userId, request.eventKey())
                .orElseGet(NotificationPreference::new);
        pref.setUser(user);
        pref.setEventKey(request.eventKey());
        pref.setInApp(request.inApp());
        pref.setEmail(request.email());
        pref.setSms(request.sms());
        pref.setPush(request.push());
        NotificationPreference saved = preferenceRepository.save(pref);
        return new NotificationPreferenceResponse(
                saved.getEventKey(), saved.isInApp(), saved.isEmail(), saved.isSms(), saved.isPush());
    }

    public boolean allow(Long userId, String eventKey, NotificationChannel channel) {
        return preferenceRepository.findByUserIdAndEventKey(userId, eventKey)
                .map(pref -> switch (channel) {
                    case IN_APP -> pref.isInApp();
                    case EMAIL -> pref.isEmail();
                    case SMS -> pref.isSms();
                    case PUSH -> pref.isPush();
                })
                .orElse(channel != NotificationChannel.EMAIL);
    }

    public String[] render(Long schoolId, String eventKey, String fallbackTitle, String fallbackBody) {
        return templateRepository.findBySchoolIdAndEventKeyAndChannel(schoolId, eventKey, NotificationChannel.IN_APP)
                .map(template -> new String[]{template.getSubject(), template.getBody()})
                .orElse(new String[]{fallbackTitle, fallbackBody});
    }

    private static NotificationTemplateResponse toTemplate(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getId(), template.getEventKey(), template.getChannel(), template.getSubject(), template.getBody());
    }
}
