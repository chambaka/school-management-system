package tz.co.chambaka.school.management.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimitService {

    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowSeconds;
    private final RedisRateLimitClient redis;

    @Autowired
    public LoginRateLimitService(
            @Value("${sms.login-rate-limit.max-attempts:8}") int maxAttempts,
            @Value("${sms.login-rate-limit.window-seconds:300}") long windowSeconds,
            @Value("${sms.redis.enabled:false}") boolean redisEnabled,
            @Value("${sms.redis.url:redis://localhost:6379}") String redisUrl
    ) {
        this(maxAttempts, windowSeconds, new RedisRateLimitClient(redisEnabled, redisUrl));
    }

    LoginRateLimitService(int maxAttempts, long windowSeconds, RedisRateLimitClient redis) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.redis = redis == null ? new RedisRateLimitClient(false, "redis://localhost:6379") : redis;
    }

    public boolean isBlocked(String key) {
        String normalized = normalize(key);
        Long redisCount = redis.getCount(normalized);
        if (redisCount != null) {
            return redisCount >= maxAttempts;
        }
        return recent(normalized).size() >= maxAttempts;
    }

    public void recordFailure(String key) {
        String normalized = normalize(key);
        if (redis.increment(normalized, windowSeconds) != null) {
            return;
        }
        recent(normalized).addLast(Instant.now().getEpochSecond());
    }

    public void clear(String key) {
        String normalized = normalize(key);
        redis.delete(normalized);
        attempts.remove(normalized);
    }

    private Deque<Long> recent(String key) {
        long cutoff = Instant.now().getEpochSecond() - windowSeconds;
        Deque<Long> times = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        while (!times.isEmpty() && times.peekFirst() < cutoff) {
            times.removeFirst();
        }
        return times;
    }

    private static String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }
}
