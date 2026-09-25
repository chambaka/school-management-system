package tz.co.chambaka.school.management.sms;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.logging.RequestContext;
import tz.co.chambaka.school.management.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CredentialSmsService {

    private static final Logger log = LoggerFactory.getLogger(CredentialSmsService.class);

    private final SmsGateway smsGateway;
    private final SmsProperties properties;

    public CredentialSmsService(SmsGateway smsGateway, SmsProperties properties) {
        this.smsGateway = smsGateway;
        this.properties = properties;
    }

    public SmsSendResult sendResetCode(User user, String code, int ttlSeconds) {
        int minutes = Math.max(1, (int) Math.round(ttlSeconds / 60.0));
        String body = "ShuleHub reset code: " + code + ". Valid for " + minutes
                + " minutes. Do not share this code.";
        return send(user, body, "reset-code");
    }

    public SmsSendResult sendTemporaryPassword(User user, String rawPassword) {
        String login = user == null || user.getEmail() == null ? "your account" : user.getEmail();
        String body = "ShuleHub login: " + login + ". Temporary password: " + rawPassword
                + ". Change it after you sign in.";
        return send(user, body, "temp-password");
    }

    private SmsSendResult send(User user, String body, String kind) {
        if (user == null) {
            log.info("credential-sms skipped kind={} reason=no-user correctionId={} ip={}",
                    kind, RequestContext.getCorrectionId(), RequestContext.getIpAddress());
            return SmsSendResult.skipped("no-user");
        }
        String phone = user.getPhone();
        if (phone == null || phone.isBlank()) {
            log.info("credential-sms skipped kind={} reason=no-phone userId={} email={} correctionId={} ip={}",
                    kind, user.getId(), user.getEmail(),
                    RequestContext.getCorrectionId(), RequestContext.getIpAddress());
            return SmsSendResult.skipped("no-phone");
        }
        String to = PhoneNumbers.toE164Like(phone);
        String sender = senderId();
        log.info("credential-sms send kind={} userId={} email={} to={} sender={} body={} correctionId={} ip={} ua={}",
                kind,
                user.getId(),
                user.getEmail(),
                PhoneNumbers.mask(to),
                sender,
                body,
                RequestContext.getCorrectionId(),
                RequestContext.getIpAddress(),
                RequestContext.getUserAgent());
        SmsSendResult result = smsGateway.send(to, sender, body);
        if (result == null) {
            log.warn("credential-sms result kind={} userId={} to={} sent=false error=null-result",
                    kind, user.getId(), PhoneNumbers.mask(to));
            return SmsSendResult.failed("null-result");
        }
        log.info("credential-sms result kind={} userId={} to={} sent={} ref={} error={}",
                kind,
                user.getId(),
                PhoneNumbers.mask(to),
                result.sent(),
                result.providerRef(),
                result.error());
        return result;
    }

    private String senderId() {
        SmsProperties.Messaging messaging = properties == null ? null : properties.messaging();
        if (messaging == null || messaging.senderId() == null || messaging.senderId().isBlank()) {
            return "SHULEHUB";
        }
        return messaging.senderId();
    }
}
