package tz.co.chambaka.school.management.ledger;

import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.Payment;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock LedgerEntryRepository ledgerEntryRepository;
    @InjectMocks LedgerService service;

    @Test
    void recordsAndListsEntries() {
        Invoice invoice = new Invoice();
        invoice.setSchoolId(1L);
        invoice.setStudent(Fixtures.student());
        invoice.setInvoiceNumber("INV-9");
        invoice.setTotalAmount(BigDecimal.TEN);
        Payment payment = new Payment();
        payment.setSchoolId(1L);
        payment.setStudent(Fixtures.student());
        payment.setInvoice(invoice);
        payment.setAmount(BigDecimal.ONE);
        payment.setReceiptNumber("R-9");
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> {
            LedgerEntry saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        assertThat(service.debitInvoice(invoice).getEntryType()).isEqualTo(LedgerEntryType.DEBIT);
        assertThat(service.creditPayment(payment).getEntryType()).isEqualTo(LedgerEntryType.CREDIT);
        assertThat(service.creditDiscount(invoice, BigDecimal.ONE).getEntryType()).isEqualTo(LedgerEntryType.ADJUSTMENT);

        LedgerEntry listed = new LedgerEntry();
        listed.setId(2L);
        listed.setStudent(Fixtures.student());
        listed.setInvoice(invoice);
        listed.setPayment(payment);
        listed.setEntryType(LedgerEntryType.CREDIT);
        listed.setAmount(BigDecimal.ONE);
        listed.setDescription("Payment");
        listed.setOccurredAt(Instant.now());
        when(ledgerEntryRepository.findBySchoolIdAndStudentIdOrderByOccurredAtDesc(1L, 1L)).thenReturn(List.of(listed));
        assertThat(service.list(1L, 1L)).hasSize(1);
        assertThat(service.list(1L, 1L).getFirst().paymentId()).isEqualTo(payment.getId());
    }
}
