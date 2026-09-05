package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findBySchoolId(Long schoolId, Pageable pageable);

    List<Invoice> findBySchoolIdAndStudentId(Long schoolId, Long studentId);

    List<Invoice> findBySchoolIdAndStudentIdAndStatusIn(Long schoolId, Long studentId, List<InvoiceStatus> statuses);

    Optional<Invoice> findByIdAndSchoolId(Long id, Long schoolId);

    long countBySchoolId(Long schoolId);
}
