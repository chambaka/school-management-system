package tz.co.chambaka.school.management.notification;

import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSettingsServiceTest {

    @Mock NotificationTemplateRepository templateRepository;
    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock UserRepository userRepository;
    @InjectMocks NotificationSettingsService service;

    @Test
    void templatesPreferencesAndAllow() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(1L);
        template.setEventKey("ATTENDANCE");
        template.setChannel(NotificationChannel.IN_APP);
        template.setSubject("Absent");
        template.setBody("Child was absent");
        when(templateRepository.findBySchoolIdOrderByEventKeyAsc(1L)).thenReturn(List.of(template));
        assertThat(service.templates(1L)).hasSize(1);

        when(templateRepository.findBySchoolIdAndEventKeyAndChannel(1L, "ATTENDANCE", NotificationChannel.IN_APP))
                .thenReturn(Optional.of(template));
        assertThat(service.render(1L, "ATTENDANCE", "t", "b")).containsExactly("Absent", "Child was absent");
        when(templateRepository.save(any(NotificationTemplate.class))).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.saveTemplate(1L, new NotificationTemplateRequest("ATTENDANCE", null, "Sub", "Body")).subject())
                .isEqualTo("Sub");
        when(templateRepository.findBySchoolIdAndEventKeyAndChannel(1L, "OTHER", NotificationChannel.IN_APP))
                .thenReturn(Optional.empty());
        assertThat(service.render(1L, "OTHER", "t", "b")).containsExactly("t", "b");

        NotificationPreference pref = new NotificationPreference();
        pref.setEventKey("ATTENDANCE");
        pref.setInApp(true);
        pref.setEmail(false);
        pref.setSms(true);
        pref.setPush(true);
        when(preferenceRepository.findByUserId(2L)).thenReturn(List.of(pref));
        assertThat(service.preferences(2L)).hasSize(1);
        when(userRepository.findById(2L)).thenReturn(Optional.of(Fixtures.user(2L, Role.HEADMASTER)));
        when(preferenceRepository.findByUserIdAndEventKey(2L, "ATTENDANCE")).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(inv -> inv.getArgument(0));
        NotificationPreferenceResponse saved = service.savePreference(
                2L, new NotificationPreferenceRequest("ATTENDANCE", true, true, false, true));
        assertThat(saved.inApp()).isTrue();
        assertThat(saved.email()).isFalse();
        assertThat(saved.sms()).isTrue();
        assertThat(saved.push()).isFalse();
        when(preferenceRepository.findByUserIdAndEventKey(2L, "ATTENDANCE")).thenReturn(Optional.of(pref));
        assertThat(service.allow(2L, "ATTENDANCE", NotificationChannel.IN_APP)).isTrue();
        assertThat(service.allow(2L, "ATTENDANCE", NotificationChannel.EMAIL)).isFalse();
        assertThat(service.allow(2L, "ATTENDANCE", NotificationChannel.SMS)).isTrue();
        assertThat(service.allow(2L, "ATTENDANCE", NotificationChannel.PUSH)).isFalse();
        when(preferenceRepository.findByUserIdAndEventKey(2L, "X")).thenReturn(Optional.empty());
        assertThat(service.allow(2L, "X", NotificationChannel.EMAIL)).isFalse();
        assertThat(service.allow(2L, "X", NotificationChannel.PUSH)).isFalse();
        assertThat(service.allow(2L, "X", NotificationChannel.SMS)).isTrue();
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.savePreference(9L, new NotificationPreferenceRequest("X", true, false, false, false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
