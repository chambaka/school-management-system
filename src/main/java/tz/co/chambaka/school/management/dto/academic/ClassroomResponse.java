package tz.co.chambaka.school.management.dto.academic;

public record ClassroomResponse(
        Long id,
        String name,
        String code,
        Integer capacity,
        Long buildingId,
        String building,
        String notes
) {
    public ClassroomResponse(Long id, String name, String code, Integer capacity, String building, String notes) {
        this(id, name, code, capacity, null, building, notes);
    }
}
