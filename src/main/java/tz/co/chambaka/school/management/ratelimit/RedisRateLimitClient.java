package tz.co.chambaka.school.management.ratelimit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Optional Redis (RESP) client for login rate-limit counters.
 * Failures return null so callers can fall back to in-memory storage.
 */
public class RedisRateLimitClient {

    private final boolean enabled;
    private final String host;
    private final int port;

    public RedisRateLimitClient(boolean enabled, String url) {
        this.enabled = enabled;
        URI uri = URI.create(url == null || url.isBlank() ? "redis://localhost:6379" : url);
        this.host = uri.getHost() == null ? "localhost" : uri.getHost();
        this.port = uri.getPort() <= 0 ? 6379 : uri.getPort();
    }

    public boolean enabled() {
        return enabled;
    }

    public Long getCount(String key) {
        if (!enabled) {
            return null;
        }
        return command(key, "GET");
    }

    public Long increment(String key, long ttlSeconds) {
        if (!enabled) {
            return null;
        }
        Long value = command(key, "INCR");
        if (value != null && value == 1L) {
            command(key, "EXPIRE", String.valueOf(Math.max(ttlSeconds, 1)));
        }
        return value;
    }

    public boolean delete(String key) {
        if (!enabled) {
            return false;
        }
        return command(key, "DEL") != null;
    }

    String host() {
        return host;
    }

    int port() {
        return port;
    }

    static String encode(String... parts) {
        StringBuilder sb = new StringBuilder();
        sb.append('*').append(parts.length).append("\r\n");
        for (String part : parts) {
            sb.append('$').append(part.getBytes(StandardCharsets.UTF_8).length).append("\r\n");
            sb.append(part).append("\r\n");
        }
        return sb.toString();
    }

    private Long command(String key, String... extra) {
        String[] parts = extra.length == 1
                ? new String[]{extra[0], prefix(key)}
                : new String[]{extra[0], prefix(key), extra[1]};
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 200);
            socket.setSoTimeout(400);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.write(encode(parts));
            out.flush();
            return readInteger(in);
        } catch (Exception ex) {
            return null;
        }
    }

    static Long readInteger(BufferedReader in) throws Exception {
        String line = in.readLine();
        if (line == null) {
            return null;
        }
        if (line.startsWith(":")) {
            return Long.parseLong(line.substring(1));
        }
        if (line.startsWith("$")) {
            int len = Integer.parseInt(line.substring(1));
            if (len < 0) {
                return 0L;
            }
            String body = in.readLine();
            return body == null ? 0L : Long.parseLong(body);
        }
        return null;
    }

    private static String prefix(String key) {
        return "sms:login:" + key;
    }
}
