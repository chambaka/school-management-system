package tz.co.chambaka.school.management.dto.academic;

import tz.co.chambaka.school.management.model.enums.PeriodKind;

import java.time.LocalTime;

public record BellPeriodResponse(
        Long id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        PeriodKind kind,
        int sortOrder
) {
}
