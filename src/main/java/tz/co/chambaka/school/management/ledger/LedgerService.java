package tz.co.chambaka.school.management.ledger;

import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.Payment;
import tz.co.chambaka.school.management.model.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public LedgerEntry debitInvoice(Invoice invoice) {
        return record(invoice.getSchoolId(), invoice.getStudent(), invoice, null,
                LedgerEntryType.DEBIT, invoice.getTotalAmount(), "Invoice " + invoice.getInvoiceNumber());
    }

    @Transactional
    public LedgerEntry creditPayment(Payment payment) {
        return record(payment.getSchoolId(), payment.getStudent(), payment.getInvoice(), payment,
                LedgerEntryType.CREDIT, payment.getAmount(), "Payment " + payment.getReceiptNumber());
    }

    @Transactional
    public LedgerEntry creditDiscount(Invoice invoice, BigDecimal amount) {
        return record(invoice.getSchoolId(), invoice.getStudent(), invoice, null,
                LedgerEntryType.ADJUSTMENT, amount, "Discount on " + invoice.getInvoiceNumber());
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> list(Long schoolId, Long studentId) {
        return ledgerEntryRepository.findBySchoolIdAndStudentIdOrderByOccurredAtDesc(schoolId, studentId)
                .stream()
                .map(entry -> new LedgerEntryResponse(
                        entry.getId(),
                        entry.getStudent().getId(),
                        entry.getInvoice() == null ? null : entry.getInvoice().getId(),
                        entry.getPayment() == null ? null : entry.getPayment().getId(),
                        entry.getEntryType(),
                        entry.getAmount(),
                        entry.getDescription(),
                        entry.getOccurredAt()))
                .toList();
    }

    private LedgerEntry record(
            Long schoolId,
            Student student,
            Invoice invoice,
            Payment payment,
            LedgerEntryType type,
            BigDecimal amount,
            String description
    ) {
        LedgerEntry entry = new LedgerEntry();
        entry.setSchoolId(schoolId);
        entry.setStudent(student);
        entry.setInvoice(invoice);
        entry.setPayment(payment);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setDescription(description);
        entry.setOccurredAt(Instant.now());
        return ledgerEntryRepository.save(entry);
    }
}
