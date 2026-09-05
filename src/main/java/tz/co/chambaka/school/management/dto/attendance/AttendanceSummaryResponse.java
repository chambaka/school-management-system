package tz.co.chambaka.school.management.dto.attendance;

public record AttendanceSummaryResponse(
        Long personId,
        String name,
        long present,
        long absent,
        long late,
        long excused,
        long total,
        double attendancePercent
) {
}
