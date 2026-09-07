package tz.co.chambaka.school.management.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsGateway.class);

    @Override
    public SmsSendResult send(String phone, String senderId, String body) {
        log.info("SMS queued provider=log sender={} to={} body={}", senderId, PhoneNumbers.mask(phone), body);
        return SmsSendResult.ok("log");
    }
}
