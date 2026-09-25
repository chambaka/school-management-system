package tz.co.chambaka.school.management.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitServiceTest {

    @Test
    void blocksAfterMaxAttemptsAndClears() {
        LoginRateLimitService service = new LoginRateLimitService(2, 300, false, "redis://localhost:6379");
        assertThat(service.isBlocked(" A@B.com ")).isFalse();
        service.recordFailure("A@B.com");
        service.recordFailure("a@b.com");
        assertThat(service.isBlocked("a@b.com")).isTrue();
        service.clear("a@b.com");
        assertThat(service.isBlocked("a@b.com")).isFalse();
        assertThat(service.isBlocked(null)).isFalse();
    }

    @Test
    void redisCountsOverrideMemory() {
        RedisRateLimitClient redis = new RedisRateLimitClient(true, "redis://127.0.0.1:1") {
            private long count;

            @Override
            public Long getCount(String key) {
                return count;
            }

            @Override
            public Long increment(String key, long ttlSeconds) {
                return ++count;
            }

            @Override
            public boolean delete(String key) {
                count = 0;
                return true;
            }
        };
        LoginRateLimitService service = new LoginRateLimitService(2, 60, redis);
        service.recordFailure("x");
        service.recordFailure("x");
        assertThat(service.isBlocked("x")).isTrue();
        service.clear("x");
        assertThat(service.isBlocked("x")).isFalse();
    }

    @Test
    void deadRedisFallsBackToMemory() {
        LoginRateLimitService service = new LoginRateLimitService(1, 300, true, "redis://127.0.0.1:1");
        assertThat(service.isBlocked("user")).isFalse();
        service.recordFailure("user");
        assertThat(service.isBlocked("user")).isTrue();
        service.clear("user");
        assertThat(service.isBlocked("user")).isFalse();
    }
}
