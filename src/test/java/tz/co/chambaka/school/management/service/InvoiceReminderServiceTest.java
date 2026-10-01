package tz.co.chambaka.school.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.co.chambaka.school.management.config.InvoiceReminderProperties;
import tz.co.chambaka.school.management.model.FeeStructure;
import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.InvoiceItem;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import tz.co.chambaka.school.management.support.Fixtures;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceReminderServiceTest {

    @Mock SchoolRepository schoolRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock AlertService alertService;

    private InvoiceReminderProperties properties;
    private InvoiceReminderService service;

    @BeforeEach
    void setUp() {
        properties = new InvoiceReminderProperties();
        service = new InvoiceReminderService(properties, schoolRepository, invoiceRepository, alertService);
    }

    @Test
    void messageNamesTheFeeAndTermStillOwed() {
        Invoice invoice = invoice();
        invoice.setPaidAmount(new BigDecimal("150000"));
        String body = InvoiceReminderService.message(invoice, "TZS");
        assertThat(body).contains("owes TZS 150000.00");
        assertThat(body).contains("Partly paid: Transport · Term 2 TZS 150000.00");
        assertThat(body).doesNotContain("Term 1");
    }

    @Test
    void remindsOpenInvoicesForActiveSchools() {
        School school = Fixtures.school();
        Invoice invoice = invoice();
        when(schoolRepository.findAll()).thenReturn(List.of(school));
        when(invoiceRepository.findOpenForReminder(eq(1L), eq(List.of(
                InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE))))
                .thenReturn(List.of(invoice));

        service.remindOpenInvoices();

        verify(alertService).remindHousehold(
                eq(1L),
                eq(invoice.getStudent()),
                eq("Fee reminder"),
                argThat(body -> body.contains("Unpaid: Tuition · Term 1 TZS 100000.00")
                        && body.contains("Unpaid: Transport · Term 2 TZS 200000.00")),
                eq(7L));
    }

    @Test
    void skipsSuspendedSchools() {
        School suspended = Fixtures.school();
        suspended.setStatus(SchoolStatus.SUSPENDED);
        when(schoolRepository.findAll()).thenReturn(List.of(suspended));
        service.remindOpenInvoices();
        verify(invoiceRepository, never()).findOpenForReminder(any(), any());
    }

    @Test
    void skipsWhenDisabled() {
        properties.setEnabled(false);
        service.remindOpenInvoices();
        verify(schoolRepository, never()).findAll();
    }

    private static Invoice invoice() {
        Invoice invoice = new Invoice();
        invoice.setId(7L);
        invoice.setInvoiceNumber("INV-1");
        invoice.setStudent(Fixtures.student());
        invoice.setTotalAmount(new BigDecimal("300000"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setItems(List.of(
                line("Tuition", "Term 1", new BigDecimal("100000")),
                line("Transport", "Term 2", new BigDecimal("200000"))));
        return invoice;
    }

    private static InvoiceItem line(String name, String period, BigDecimal amount) {
        FeeStructure fee = new FeeStructure();
        fee.setName(name);
        fee.setPeriodLabel(period);
        InvoiceItem item = new InvoiceItem();
        item.setFeeStructure(fee);
        item.setDescription(name + " · " + period);
        item.setAmount(amount);
        return item;
    }
}
