package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.config.InvoiceReminderProperties;
import tz.co.chambaka.school.management.model.FeeStructure;
import tz.co.chambaka.school.management.model.Invoice;
import tz.co.chambaka.school.management.model.InvoiceItem;
import tz.co.chambaka.school.management.model.School;
import tz.co.chambaka.school.management.model.Student;
import tz.co.chambaka.school.management.model.enums.InvoiceStatus;
import tz.co.chambaka.school.management.model.enums.SchoolStatus;
import tz.co.chambaka.school.management.repository.InvoiceRepository;
import tz.co.chambaka.school.management.repository.SchoolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class InvoiceReminderService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceReminderService.class);
    private static final List<InvoiceStatus> OPEN = List.copyOf(
            EnumSet.of(InvoiceStatus.PENDING, InvoiceStatus.PARTIAL, InvoiceStatus.OVERDUE));

    private final InvoiceReminderProperties properties;
    private final SchoolRepository schoolRepository;
    private final InvoiceRepository invoiceRepository;
    private final AlertService alertService;

    public InvoiceReminderService(
            InvoiceReminderProperties properties,
            SchoolRepository schoolRepository,
            InvoiceRepository invoiceRepository,
            AlertService alertService
    ) {
        this.properties = properties;
        this.schoolRepository = schoolRepository;
        this.invoiceRepository = invoiceRepository;
        this.alertService = alertService;
    }

    @Transactional
    public void remindOpenInvoices() {
        if (!properties.isEnabled()) {
            return;
        }
        for (School school : schoolRepository.findAll()) {
            if (school.getId() == null || !openSchool(school)) {
                continue;
            }
            try {
                remindSchool(school);
            } catch (RuntimeException ex) {
                log.warn("Invoice reminders failed schoolId={}", school.getId(), ex);
            }
        }
    }

    private void remindSchool(School school) {
        String currency = school.getCurrency() == null || school.getCurrency().isBlank() ? "TZS" : school.getCurrency();
        for (Invoice invoice : invoiceRepository.findOpenForReminder(school.getId(), OPEN)) {
            try {
                remindInvoice(school.getId(), currency, invoice);
            } catch (RuntimeException ex) {
                log.warn("Invoice reminder failed schoolId={} invoiceId={}", school.getId(), invoice.getId(), ex);
            }
        }
    }

    private void remindInvoice(Long schoolId, String currency, Invoice invoice) {
        if (invoice == null || invoice.getStudent() == null || invoice.getBalance() == null || invoice.getBalance().signum() <= 0) {
            return;
        }
        String body = message(invoice, currency);
        alertService.remindHousehold(schoolId, invoice.getStudent(), "Fee reminder", body, invoice.getId());
        log.info("Invoice reminder schoolId={} invoiceId={} studentId={}", schoolId, invoice.getId(), invoice.getStudent().getId());
    }

    static String message(Invoice invoice, String currency) {
        Student student = invoice.getStudent();
        String name = studentName(student);
        String owed = money(invoice.getBalance());
        List<String> lines = new ArrayList<>();
        for (LineDue line : unpaidLines(invoice)) {
            String state = line.partial() ? "Partly paid" : "Unpaid";
            lines.add(state + ": " + line.label() + " " + currency + " " + money(line.remaining()));
        }
        if (lines.isEmpty()) {
            return name + " owes " + currency + " " + owed + " on invoice " + invoice.getInvoiceNumber() + ".";
        }
        return name + " owes " + currency + " " + owed + ". " + String.join(". ", lines) + ".";
    }

    static List<LineDue> unpaidLines(Invoice invoice) {
        BigDecimal credit = zero(invoice.getPaidAmount()).add(zero(invoice.getDiscountAmount()));
        List<LineDue> lines = new ArrayList<>();
        for (InvoiceItem item : invoice.getItems() == null ? List.<InvoiceItem>of() : invoice.getItems()) {
            BigDecimal amount = zero(item.getAmount());
            BigDecimal covered = credit.min(amount);
            if (covered.signum() < 0) {
                covered = BigDecimal.ZERO;
            }
            BigDecimal remaining = amount.subtract(covered);
            credit = credit.subtract(covered);
            if (remaining.signum() > 0) {
                lines.add(new LineDue(feeLabel(item), remaining, covered.signum() > 0));
            }
        }
        return lines;
    }

    static String feeLabel(InvoiceItem item) {
        FeeStructure fee = item.getFeeStructure();
        if (fee != null) {
            String name = fee.getName() != null && !fee.getName().isBlank() ? fee.getName() : item.getDescription();
            String period = fee.getPeriodLabel();
            if (name != null && period != null && !period.isBlank()) {
                return name.trim() + " · " + period.trim();
            }
            if (item.getDescription() != null && !item.getDescription().isBlank()) {
                return item.getDescription().trim();
            }
            return name == null ? "Fee" : name.trim();
        }
        if (item.getDescription() != null && !item.getDescription().isBlank()) {
            return item.getDescription().trim();
        }
        return "Fee";
    }

    private static String studentName(Student student) {
        if (student.getUser() != null && student.getUser().displayName() != null && !student.getUser().displayName().isBlank()) {
            return student.getUser().displayName();
        }
        return student.getAdmissionNo() == null ? "Student" : student.getAdmissionNo();
    }

    private static boolean openSchool(School school) {
        return school.getStatus() == SchoolStatus.ACTIVE || school.getStatus() == SchoolStatus.TRIAL;
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String money(BigDecimal amount) {
        return zero(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    record LineDue(String label, BigDecimal remaining, boolean partial) {
    }
}
