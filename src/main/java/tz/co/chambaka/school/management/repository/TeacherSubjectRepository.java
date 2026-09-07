package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    @Query("""
            SELECT DISTINCT a FROM TeacherSubject a
            JOIN FETCH a.teacher t
            JOIN FETCH t.user
            JOIN FETCH a.subject
            JOIN FETCH a.schoolClass
            LEFT JOIN FETCH a.section
            JOIN FETCH a.academicYear
            WHERE a.schoolId = :schoolId
            """)
    List<TeacherSubject> findBySchoolId(@Param("schoolId") Long schoolId);

    @Query("""
            SELECT DISTINCT a FROM TeacherSubject a
            JOIN FETCH a.teacher t
            JOIN FETCH t.user
            JOIN FETCH a.subject
            JOIN FETCH a.schoolClass
            LEFT JOIN FETCH a.section
            JOIN FETCH a.academicYear
            WHERE a.schoolId = :schoolId AND a.academicYear.id = :academicYearId
            """)
    List<TeacherSubject> findBySchoolIdAndAcademicYearId(
            @Param("schoolId") Long schoolId, @Param("academicYearId") Long academicYearId);

    @Query("""
            SELECT DISTINCT a FROM TeacherSubject a
            JOIN FETCH a.teacher t
            JOIN FETCH t.user
            JOIN FETCH a.subject
            JOIN FETCH a.schoolClass
            LEFT JOIN FETCH a.section
            JOIN FETCH a.academicYear
            WHERE a.schoolId = :schoolId AND t.id = :teacherId
            """)
    List<TeacherSubject> findBySchoolIdAndTeacherId(
            @Param("schoolId") Long schoolId, @Param("teacherId") Long teacherId);

    Optional<TeacherSubject> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsByTeacherIdAndSubjectIdAndSchoolClassIdAndSectionIdAndAcademicYearId(
            Long teacherId, Long subjectId, Long schoolClassId, Long sectionId, Long academicYearId);

    void deleteBySchoolClassId(Long schoolClassId);

    void deleteBySectionId(Long sectionId);

    void deleteBySubjectId(Long subjectId);
}
