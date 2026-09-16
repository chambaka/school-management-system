package tz.co.chambaka.school.management.dto.academic;

import java.math.BigDecimal;

public record GradingBandResponse(
        Long id,
        int minPercent,
        int maxPercent,
        String letter,
        BigDecimal points,
        int sortOrder
) {
}
