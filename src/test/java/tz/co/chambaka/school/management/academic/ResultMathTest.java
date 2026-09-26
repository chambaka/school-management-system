package tz.co.chambaka.school.management.academic;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ResultMathTest {

    @Test
    void appendixAWorkedExample() {
        BigDecimal semi = ResultMath.semiTerminal(new BigDecimal("78"), new BigDecimal("65"),
                new BigDecimal("10"), new BigDecimal("90"));
        assertThat(semi).isEqualByComparingTo("66.30");
        BigDecimal terminal = ResultMath.terminal(semi, new BigDecimal("72"),
                new BigDecimal("50"), new BigDecimal("50"));
        assertThat(terminal).isEqualByComparingTo("69.15");
    }

    @Test
    void aMissingExamIsLeftOutSoTheMarkTakenStands() {
        BigDecimal semi = ResultMath.semiTerminal(new BigDecimal("95"), null,
                new BigDecimal("10"), new BigDecimal("90"));
        assertThat(semi).isEqualByComparingTo("95.00");
        BigDecimal terminal = ResultMath.terminal(semi, null, new BigDecimal("50"), new BigDecimal("50"));
        assertThat(terminal).isEqualByComparingTo("95.00");
        assertThat(ResultMath.letter(terminal, null)).isEqualTo("A");
    }

    @Test
    void aZeroMarkIsCounted() {
        BigDecimal semi = ResultMath.semiTerminal(new BigDecimal("95"), BigDecimal.ZERO,
                new BigDecimal("10"), new BigDecimal("90"));
        assertThat(semi).isEqualByComparingTo("9.50");
    }
}
