package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findBySchoolIdAndInvoiceId(Long schoolId, Long invoiceId);

    List<Payment> findBySchoolIdAndStudentId(Long schoolId, Long studentId);

    Optional<Payment> findByIdAndSchoolId(Long id, Long schoolId);

    long countBySchoolId(Long schoolId);
}
