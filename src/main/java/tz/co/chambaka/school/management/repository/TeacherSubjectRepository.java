package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    List<TeacherSubject> findBySchoolId(Long schoolId);

    List<TeacherSubject> findBySchoolIdAndAcademicYearId(Long schoolId, Long academicYearId);

    List<TeacherSubject> findBySchoolIdAndTeacherId(Long schoolId, Long teacherId);

    Optional<TeacherSubject> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsByTeacherIdAndSubjectIdAndSchoolClassIdAndSectionIdAndAcademicYearId(
            Long teacherId, Long subjectId, Long schoolClassId, Long sectionId, Long academicYearId);

    void deleteBySchoolClassId(Long schoolClassId);

    void deleteBySectionId(Long sectionId);

    void deleteBySubjectId(Long subjectId);
}
