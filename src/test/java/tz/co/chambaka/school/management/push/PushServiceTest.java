package tz.co.chambaka.school.management.push;

import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.enums.Role;
import tz.co.chambaka.school.management.repository.UserRepository;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    @Mock PushSubscriptionRepository subscriptionRepository;
    @Mock UserRepository userRepository;

    @Test
    void subscribeDispatchAndUnsubscribe() {
        PushService service = new PushService(subscriptionRepository, userRepository, "vapid");
        when(userRepository.findById(2L)).thenReturn(Optional.of(Fixtures.user(2L, Role.HEADMASTER)));
        when(subscriptionRepository.findByUserIdAndEndpoint(2L, "https://push")).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(PushSubscription.class))).thenAnswer(inv -> inv.getArgument(0));
        service.subscribe(2L, new PushSubscribeRequest("https://push", "k", "a"));
        PushSubscription existing = new PushSubscription();
        when(subscriptionRepository.findByUserId(2L)).thenReturn(List.of(existing));
        assertThat(service.dispatch(2L, "Hi", "Body")).isEqualTo(1);
        assertThat(service.publicKey()).isEqualTo("vapid");
        service.unsubscribe(2L, "https://push");
        verify(subscriptionRepository).deleteByUserIdAndEndpoint(2L, "https://push");

        PushService noKey = new PushService(subscriptionRepository, userRepository, "");
        assertThat(noKey.dispatch(2L, "Hi", "Body")).isZero();
        when(userRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.subscribe(9L, new PushSubscribeRequest("e", "k", "a")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
