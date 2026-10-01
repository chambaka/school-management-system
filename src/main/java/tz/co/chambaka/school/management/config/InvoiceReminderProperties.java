package tz.co.chambaka.school.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sms.invoice-reminder")
public class InvoiceReminderProperties {

    private boolean enabled = true;
    private String cron = "0 0 8 * * *";
    private String zone = "Africa/Dar_es_Salaam";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
