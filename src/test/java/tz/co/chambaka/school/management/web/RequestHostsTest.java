package tz.co.chambaka.school.management.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestHostsTest {

    @Test
    void recognizesLoopbackHostsWithAndWithoutPort() {
        assertThat(RequestHosts.isLoopback("localhost:5174")).isTrue();
        assertThat(RequestHosts.isLoopback("LOCALHOST")).isTrue();
        assertThat(RequestHosts.isLoopback("127.0.0.1:8989")).isTrue();
        assertThat(RequestHosts.isLoopback("0.0.0.0")).isTrue();
        assertThat(RequestHosts.isLoopback("[::1]:5173")).isTrue();
        assertThat(RequestHosts.isLoopback("::1")).isTrue();
        assertThat(RequestHosts.isLoopback("school.halo.tz")).isFalse();
        assertThat(RequestHosts.isLoopback("")).isFalse();
        assertThat(RequestHosts.isLoopback(null)).isFalse();
    }

    @Test
    void extractsHostname() {
        assertThat(RequestHosts.hostname(" localhost:5174 ")).isEqualTo("localhost");
        assertThat(RequestHosts.hostname("[::1]:5173")).isEqualTo("::1");
        assertThat(RequestHosts.hostname("[bad")).isEqualTo("[bad");
        assertThat(RequestHosts.hostname(null)).isEmpty();
    }
}
