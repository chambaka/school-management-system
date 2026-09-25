package tz.co.chambaka.school.management.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findBySchoolIdAndStudentIdOrderByOccurredAtDesc(Long schoolId, Long studentId);
}
