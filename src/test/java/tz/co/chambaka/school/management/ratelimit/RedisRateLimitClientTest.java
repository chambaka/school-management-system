package tz.co.chambaka.school.management.ratelimit;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class RedisRateLimitClientTest {

    @Test
    void disabledClientIsNoopAndParsesUrl() {
        RedisRateLimitClient off = new RedisRateLimitClient(false, " ");
        assertThat(off.enabled()).isFalse();
        assertThat(off.getCount("a")).isNull();
        assertThat(off.increment("a", 10)).isNull();
        assertThat(off.delete("a")).isFalse();
        assertThat(off.host()).isEqualTo("localhost");
        assertThat(off.port()).isEqualTo(6379);
        RedisRateLimitClient named = new RedisRateLimitClient(true, "redis://cache.local:6380");
        assertThat(named.host()).isEqualTo("cache.local");
        assertThat(named.port()).isEqualTo(6380);
        assertThat(RedisRateLimitClient.encode("GET", "k")).startsWith("*2");
    }

    @Test
    void readsIntegerAndBulkAndNil() throws Exception {
        assertThat(RedisRateLimitClient.readInteger(reader(":7\r\n"))).isEqualTo(7L);
        assertThat(RedisRateLimitClient.readInteger(reader("$1\r\n4\r\n"))).isEqualTo(4L);
        assertThat(RedisRateLimitClient.readInteger(reader("$-1\r\n"))).isZero();
        assertThat(RedisRateLimitClient.readInteger(reader("-ERR\r\n"))).isNull();
        assertThat(RedisRateLimitClient.readInteger(reader(""))).isNull();
        assertThat(RedisRateLimitClient.readInteger(reader("$1\r\n"))).isZero();
    }

    @Test
    void talksToRedisAndFallsBackWhenDown() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            ExecutorService pool = Executors.newSingleThreadExecutor();
            pool.submit(() -> {
                while (!server.isClosed()) {
                    try (Socket socket = server.accept()) {
                        socket.getInputStream().read(new byte[64]);
                        socket.getOutputStream().write(":1\r\n".getBytes(StandardCharsets.UTF_8));
                    } catch (Exception ignored) {
                        return;
                    }
                }
            });
            RedisRateLimitClient client = new RedisRateLimitClient(true, "redis://127.0.0.1:" + server.getLocalPort());
            assertThat(client.increment("a@b.com", 30)).isEqualTo(1L);
            assertThat(client.getCount("a@b.com")).isEqualTo(1L);
            assertThat(client.delete("a@b.com")).isTrue();
            pool.shutdownNow();
        }
        RedisRateLimitClient dead = new RedisRateLimitClient(true, "redis://127.0.0.1:1");
        assertThat(dead.getCount("x")).isNull();
        assertThat(dead.increment("x", 10)).isNull();
        assertThat(dead.delete("x")).isFalse();
    }

    private static BufferedReader reader(String payload) {
        return new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }
}
