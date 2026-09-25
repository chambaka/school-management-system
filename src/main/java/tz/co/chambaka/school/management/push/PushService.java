package tz.co.chambaka.school.management.push;

import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.User;
import tz.co.chambaka.school.management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final String vapidPublic;

    public PushService(
            PushSubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            @Value("${sms.push.vapid-public:}") String vapidPublic
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.vapidPublic = vapidPublic;
    }

    @Transactional
    public void subscribe(Long userId, PushSubscribeRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        PushSubscription subscription = subscriptionRepository.findByUserIdAndEndpoint(userId, request.endpoint())
                .orElseGet(PushSubscription::new);
        subscription.setUser(user);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.p256dh());
        subscription.setAuth(request.auth());
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        subscriptionRepository.deleteByUserIdAndEndpoint(userId, endpoint);
    }

    @Transactional(readOnly = true)
    public String publicKey() {
        return vapidPublic;
    }

    @Transactional(readOnly = true)
    public int dispatch(Long userId, String title, String body) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserId(userId);
        if (vapidPublic == null || vapidPublic.isBlank()) {
            log.info("Push skipped (no VAPID key) userId={} count={}", userId, subscriptions.size());
            return 0;
        }
        log.info("Push queued userId={} title={} subscriptions={}", userId, title, subscriptions.size());
        return subscriptions.size();
    }
}
