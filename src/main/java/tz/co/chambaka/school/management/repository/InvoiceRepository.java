package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findBySchoolId(Long schoolId, Pageable pageable);

    List<Invoice> findBySchoolId(Long schoolId);

    List<Invoice> findBySchoolIdAndStatusIn(Long schoolId, List<InvoiceStatus> statuses);

    List<Invoice> findBySchoolIdAndStudentId(Long schoolId, Long studentId);

    List<Invoice> findBySchoolIdAndStudentIdAndStatusIn(Long schoolId, Long studentId, List<InvoiceStatus> statuses);

    Optional<Invoice> findByIdAndSchoolId(Long id, Long schoolId);

    long countBySchoolId(Long schoolId);

    boolean existsBySchoolIdAndStudentIdAndAcademicYearIdAndBillingQuarterAndStatusNot(
            Long schoolId, Long studentId, Long academicYearId, Integer billingQuarter, InvoiceStatus status);

    boolean existsByAcademicYearId(Long academicYearId);

    @Query("select count(item) > 0 from InvoiceItem item where item.feeStructure.id = :feeStructureId")
    boolean existsItemForFeeStructure(@Param("feeStructureId") Long feeStructureId);

    @Query("""
            select count(item) > 0 from InvoiceItem item
            where item.invoice.schoolId = :schoolId
              and item.invoice.student.id = :studentId
              and item.invoice.academicYear.id = :academicYearId
              and item.feeStructure.id = :feeStructureId
              and item.invoice.status <> :status
            """)
    boolean existsOpenItemForStudentFee(
            @Param("schoolId") Long schoolId,
            @Param("studentId") Long studentId,
            @Param("academicYearId") Long academicYearId,
            @Param("feeStructureId") Long feeStructureId,
            @Param("status") InvoiceStatus status);
}
