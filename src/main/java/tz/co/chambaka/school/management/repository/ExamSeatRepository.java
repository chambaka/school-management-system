package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.ExamSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamSeatRepository extends JpaRepository<ExamSeat, Long> {

    List<ExamSeat> findByExamSubjectIdOrderBySeatNumberAsc(Long examSubjectId);

    void deleteByExamSubjectId(Long examSubjectId);
}
