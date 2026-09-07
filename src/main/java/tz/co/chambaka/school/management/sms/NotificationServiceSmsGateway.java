package tz.co.chambaka.school.management.sms;

import tz.co.chambaka.school.management.config.SmsProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Delivers SMS by queuing through Chambaka notification-service
 * ({@code POST /f1/queueNotification}), same as soleiltech.
 */
public class NotificationServiceSmsGateway implements SmsGateway {

    static final String TAG = "STUDENT_MESSAGE";

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceSmsGateway.class);

    private final SmsProperties.Messaging messaging;
    private final RestClient restClient;

    public NotificationServiceSmsGateway(SmsProperties.Messaging messaging, RestClient restClient) {
        this.messaging = messaging;
        this.restClient = restClient;
    }

    @Override
    public SmsSendResult send(String phone, String senderId, String body) {
        String to = PhoneNumbers.toE164Like(phone);
        if (to == null) {
            return SmsSendResult.failed("no-phone");
        }
        if (body == null || body.isBlank()) {
            return SmsSendResult.failed("empty-body");
        }
        String baseUrl = trimTrailingSlash(messaging.baseUrl());
        if (baseUrl == null || baseUrl.isBlank()) {
            log.warn("Notification service base URL not configured");
            return SmsSendResult.failed("Notification service URL not configured");
        }
        if (messaging.apiKey() == null || messaging.apiKey().isBlank()) {
            log.warn("notification.service.api-key is empty; SMS not dispatched for to={}", PhoneNumbers.mask(to));
            return SmsSendResult.failed("api-key empty");
        }

        String path = messaging.path().startsWith("/") ? messaging.path() : "/" + messaging.path();
        String url = baseUrl + path;
        String sender = (senderId != null && !senderId.isBlank()) ? senderId.trim() : messaging.senderId();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "SMS");
        payload.put("to", to);
        payload.put("body", body);
        payload.put("senderName", sender);
        payload.put("tag", TAG);
        payload.put("idempotencyKey", "shulehub-sms-" + TAG + "-" + to + "-" + System.currentTimeMillis());
        if (messaging.provider() != null && !messaging.provider().isBlank()) {
            payload.put("provider", messaging.provider().trim());
        }

        log.info("SMS queue start to={} url={} sender={} provider={}",
                PhoneNumbers.mask(to),
                url,
                sender,
                messaging.provider() == null || messaging.provider().isBlank()
                        ? "(notification-service default)"
                        : messaging.provider());

        try {
            QueueResponse response = restClient.post()
                    .uri(url)
                    .header("X-API-Key", messaging.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(QueueResponse.class);

            if (response == null) {
                log.error("SMS queue failed to={}: empty JSON body from {}", PhoneNumbers.mask(to), url);
                return SmsSendResult.failed("Notification service returned empty body");
            }
            if (!Boolean.FALSE.equals(response.error())) {
                log.warn("SMS queue rejected to={} error={}", PhoneNumbers.mask(to), response.errorMessage());
                return SmsSendResult.failed("Notification service: " + nullToEmpty(response.errorMessage()));
            }

            log.info("SMS queued to={} message={}", PhoneNumbers.mask(to), response.errorMessage());
            return SmsSendResult.ok("queued");
        } catch (RestClientResponseException ex) {
            String responseBody = truncate(ex.getResponseBodyAsString());
            log.error("SMS queue HTTP {} to={} url={} response={}",
                    ex.getStatusCode().value(), PhoneNumbers.mask(to), url, responseBody);
            return SmsSendResult.failed("Notification service HTTP "
                    + ex.getStatusCode().value() + " " + responseBody);
        } catch (ResourceAccessException ex) {
            log.error("SMS queue connection failed to={} url={}: {}",
                    PhoneNumbers.mask(to), url, ex.toString());
            return SmsSendResult.failed("Cannot reach notification service at " + url + ": " + ex.getMessage());
        } catch (Exception ex) {
            log.error("SMS queue failed to={} url={}: {}", PhoneNumbers.mask(to), url, ex.toString(), ex);
            String detail = ex.getMessage() == null ? "http-error" : ex.getMessage();
            return SmsSendResult.failed(ex.getClass().getSimpleName() + ": " + detail);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() > 400 ? compact.substring(0, 400) : compact;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record QueueResponse(Boolean error, String errorMessage, Object data) {
    }
}
