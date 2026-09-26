package tz.co.chambaka.school.management.academic;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ResultMath {

    public static final BigDecimal DEFAULT_MIDTERM_WEIGHT = new BigDecimal("10");
    public static final BigDecimal DEFAULT_SEMI_EXAM_WEIGHT = new BigDecimal("90");
    public static final BigDecimal DEFAULT_SEMI_RESULT_WEIGHT = new BigDecimal("50");
    public static final BigDecimal DEFAULT_TERMINAL_EXAM_WEIGHT = new BigDecimal("50");

    private ResultMath() {
    }

    public static BigDecimal weighted(BigDecimal score, BigDecimal percentWeight) {
        if (score == null) {
            return BigDecimal.ZERO;
        }
        return score.multiply(percentWeight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal semiTerminal(BigDecimal midterm, BigDecimal semiExam, BigDecimal midtermWeight, BigDecimal semiExamWeight) {
        return combine(midterm, midtermWeight, semiExam, semiExamWeight);
    }

    public static BigDecimal terminal(BigDecimal semiTerminalResult, BigDecimal terminalExam, BigDecimal semiResultWeight, BigDecimal terminalExamWeight) {
        return combine(semiTerminalResult, semiResultWeight, terminalExam, terminalExamWeight);
    }

    /**
     * Mixes two scores by weight. A missing score is left out, so the score that was taken stands on its own.
     * A zero is a real mark and is included.
     */
    static BigDecimal combine(BigDecimal left, BigDecimal leftWeight, BigDecimal right, BigDecimal rightWeight) {
        if (left == null && right == null) {
            return null;
        }
        if (left == null) {
            return right.setScale(2, RoundingMode.HALF_UP);
        }
        if (right == null) {
            return left.setScale(2, RoundingMode.HALF_UP);
        }
        return weighted(left, leftWeight).add(weighted(right, rightWeight)).setScale(2, RoundingMode.HALF_UP);
    }

    public static String letter(BigDecimal percentage, Iterable<GradeBand> bands) {
        int value = percentage == null ? 0 : percentage.setScale(0, RoundingMode.DOWN).intValue();
        if (bands != null) {
            for (GradeBand band : bands) {
                if (value >= band.minPercent() && value <= band.maxPercent()) {
                    return band.letter();
                }
            }
        }
        if (value >= 80) {
            return "A";
        }
        if (value >= 70) {
            return "B";
        }
        if (value >= 60) {
            return "C";
        }
        if (value >= 50) {
            return "D";
        }
        return "F";
    }

    public static BigDecimal gpaPoints(String letter) {
        return switch (letter == null ? "F" : letter) {
            case "A" -> new BigDecimal("5.00");
            case "B" -> new BigDecimal("4.00");
            case "C" -> new BigDecimal("3.00");
            case "D" -> new BigDecimal("2.00");
            default -> BigDecimal.ZERO.setScale(2);
        };
    }

    public record GradeBand(int minPercent, int maxPercent, String letter, BigDecimal points) {
    }
}
