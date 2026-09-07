package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.TimetableSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {

    List<TimetableSlot> findBySchoolIdAndSectionIdOrderByDayOfWeekAscStartTimeAsc(Long schoolId, Long sectionId);

    List<TimetableSlot> findBySchoolIdAndTeacherIdOrderByDayOfWeekAscStartTimeAsc(Long schoolId, Long teacherId);

    Optional<TimetableSlot> findByIdAndSchoolId(Long id, Long schoolId);

    List<TimetableSlot> findBySectionIdAndDayOfWeek(Long sectionId, DayOfWeek dayOfWeek);

    void deleteBySectionId(Long sectionId);

    void deleteBySubjectId(Long subjectId);

    List<TimetableSlot> findBySchoolIdAndRoomIgnoreCase(Long schoolId, String room);

    boolean existsBySchoolIdAndRoomIgnoreCase(Long schoolId, String room);
}
