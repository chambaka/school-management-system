package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.audit.AuditService;
import tz.co.chambaka.school.management.dto.auth.VerifyTwoFactorRequest;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.TwoFactorChallenge;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.repository.TenantRepository;
import tz.co.chambaka.school.management.repository.TwoFactorChallengeRepository;
import tz.co.chambaka.school.management.security.TotpService;
import tz.co.chambaka.school.management.support.Fixtures;
import tz.co.chambaka.school.management.util.TokenHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private TwoFactorChallengeRepository challengeRepository;
    @Mock
    private TotpService totpService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private TwoFactorService service;

    @Test
    void isRequiredOnlyWhenThatSchoolHasItOn() {
        School school = Fixtures.school();
        assertThat(service.isRequired(null)).isFalse();
        assertThat(service.isRequired(school)).isFalse();
        school.setTwoFactorEnabled(true);
        assertThat(service.isRequired(school)).isTrue();
    }

    @Test
    void listsAndTogglesPerSchool() {
        School school = Fixtures.school();
        when(schoolRepository.findAll(Sort.by("name"))).thenReturn(List.of(school));
        when(tenantRepository.findAll()).thenReturn(List.of(Fixtures.tenant()));
        var listed = service.settings();
        assertThat(listed.name()).isEqualTo("ShuleHub 2FA");
        assertThat(listed.schools()).hasSize(1);
        assertThat(listed.schools().get(0).enabled()).isFalse();

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(tenantRepository.findById(Fixtures.TENANT_ID)).thenReturn(Optional.of(Fixtures.tenant()));
        assertThat(service.updateEnabled(1L, true).enabled()).isTrue();
        assertThat(school.isTwoFactorEnabled()).isTrue();
        verify(auditService).record(any());
        assertThatThrownBy(() -> service.updateEnabled(99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void startChallengeIssuesSetupKeyForFirstLogin() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        when(totpService.newSecret()).thenReturn("JBSWY3DPEHPK3PXP");
        when(totpService.otpauthUri(user.getEmail(), "JBSWY3DPEHPK3PXP")).thenReturn("otpauth://totp/test");
        var response = service.startChallenge(user);
        assertThat(response.twoFactorRequired()).isTrue();
        assertThat(response.setupRequired()).isTrue();
        assertThat(response.totpSecret()).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(response.accessToken()).isNull();
        ArgumentCaptor<TwoFactorChallenge> captor = ArgumentCaptor.forClass(TwoFactorChallenge.class);
        verify(challengeRepository).save(captor.capture());
        assertThat(captor.getValue().getPendingSecret()).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(captor.getValue().getTokenHash()).isEqualTo(TokenHash.sha256(response.pendingToken()));
    }

    @Test
    void enrolledUserDoesNotReceiveANewSecret() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        user.setTotpEnabled(true);
        user.setTotpSecret("EXISTINGSECRET");
        var response = service.startChallenge(user);
        assertThat(response.setupRequired()).isFalse();
        assertThat(response.totpSecret()).isNull();
        ArgumentCaptor<TwoFactorChallenge> captor = ArgumentCaptor.forClass(TwoFactorChallenge.class);
        verify(challengeRepository).save(captor.capture());
        assertThat(captor.getValue().getPendingSecret()).isNull();
    }

    @Test
    void verifyEnrollsPendingSecret() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        TwoFactorChallenge challenge = new TwoFactorChallenge();
        challenge.setUser(user);
        challenge.setPendingSecret("PENDINGSECRET");
        challenge.setExpiresAt(Instant.now().plusSeconds(300));
        challenge.setConsumed(false);
        when(challengeRepository.findByTokenHashAndConsumedFalse(TokenHash.sha256("token")))
                .thenReturn(Optional.of(challenge));
        when(totpService.verify("PENDINGSECRET", "123456")).thenReturn(true);

        User verified = service.verify(new VerifyTwoFactorRequest("token", "123456"));
        assertThat(verified.isTotpEnabled()).isTrue();
        assertThat(verified.getTotpSecret()).isEqualTo("PENDINGSECRET");
        assertThat(challenge.isConsumed()).isTrue();
        assertThat(challenge.getPendingSecret()).isNull();
    }

    @Test
    void verifyRejectsWrongCodeThenExpiresAfterFiveTries() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        user.setTotpEnabled(true);
        user.setTotpSecret("EXISTINGSECRET");
        TwoFactorChallenge challenge = new TwoFactorChallenge();
        challenge.setUser(user);
        challenge.setExpiresAt(Instant.now().plusSeconds(300));
        when(challengeRepository.findByTokenHashAndConsumedFalse(any())).thenReturn(Optional.of(challenge));
        when(totpService.verify("EXISTINGSECRET", "000000")).thenReturn(false);

        for (int i = 1; i < TwoFactorService.MAX_ATTEMPTS; i++) {
            assertThatThrownBy(() -> service.verify(new VerifyTwoFactorRequest("token", "000000")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage(TwoFactorService.INVALID_CODE);
            assertThat(challenge.isConsumed()).isFalse();
        }
        assertThatThrownBy(() -> service.verify(new VerifyTwoFactorRequest("token", "000000")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(TwoFactorService.INVALID_CODE);
        assertThat(challenge.isConsumed()).isTrue();
    }

    @Test
    void resetClearsEnrollmentAndPendingChallenges() {
        User user = Fixtures.user(2L, Role.HEADMASTER);
        user.setTotpEnabled(true);
        user.setTotpSecret("EXISTINGSECRET");
        service.reset(user);
        assertThat(user.isTotpEnabled()).isFalse();
        assertThat(user.getTotpSecret()).isNull();
        verify(challengeRepository).deleteByUserId(2L);
    }
}
