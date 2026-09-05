package tz.co.chambaka.school.management.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHashTest {

    @Test
    void hashesDeterministically() {
        assertThat(TokenHash.sha256("abc")).isEqualTo(TokenHash.sha256("abc"));
        assertThat(TokenHash.sha256("abc")).hasSize(64);
        assertThat(TokenHash.sha256("abc")).isNotEqualTo(TokenHash.sha256("abd"));
    }
}
