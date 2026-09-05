package tz.co.chambaka.school.management.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilTest {

    @Test
    void slugifiesSchoolName() {
        assertThat(SlugUtil.slugify("St. Mary's Primary")).isEqualTo("st-marys-primary");
    }

    @Test
    void handlesBlank() {
        assertThat(SlugUtil.slugify("  ")).isEqualTo("school");
    }
}
