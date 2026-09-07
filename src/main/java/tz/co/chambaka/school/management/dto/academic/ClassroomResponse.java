package tz.co.chambaka.school.management.dto.academic;

public record ClassroomResponse(
        Long id,
        String name,
        String code,
        Integer capacity,
        String building,
        String notes
) {
}
