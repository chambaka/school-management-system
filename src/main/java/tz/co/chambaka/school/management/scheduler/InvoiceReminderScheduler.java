package tz.co.chambaka.school.management.scheduler;

import tz.co.chambaka.school.management.config.InvoiceReminderProperties;
import tz.co.chambaka.school.management.service.InvoiceReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InvoiceReminderScheduler {

    private final InvoiceReminderProperties properties;
    private final InvoiceReminderService invoiceReminderService;

    public InvoiceReminderScheduler(InvoiceReminderProperties properties, InvoiceReminderService invoiceReminderService) {
        this.properties = properties;
        this.invoiceReminderService = invoiceReminderService;
    }

    @Scheduled(cron = "${sms.invoice-reminder.cron:0 0 8 * * *}", zone = "${sms.invoice-reminder.zone:Africa/Dar_es_Salaam}")
    public void remind() {
        if (!properties.isEnabled()) {
            return;
        }
        invoiceReminderService.remindOpenInvoices();
    }
}
