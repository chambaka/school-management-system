package tz.co.chambaka.school.management.dto.tenant;

import java.math.BigDecimal;

public record SchoolOverviewResponse(
        Long schoolId,
        long students,
        long teachers,
        long unpublishedExams,
        BigDecimal outstandingFees
) {
}
