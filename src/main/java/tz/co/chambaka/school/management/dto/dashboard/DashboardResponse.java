package tz.co.chambaka.school.management.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long students,
        long teachers,
        long invoices,
        long notices,
        long exams,
        long pendingApprovals,
        long unpublishedExams,
        long absentToday,
        BigDecimal outstandingFees,
        List<String> tasks
) {
}
