package tz.co.chambaka.school.management.dto.academic;

public record ExamSeatResponse(
        Long id,
        Long examSubjectId,
        Long studentId,
        String studentName,
        String admissionNo,
        String seatNumber
) {
}
