package tz.co.chambaka.school.management.sms;

import tz.co.chambaka.school.management.config.SmsProperties;
import tz.co.chambaka.school.management.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmsGatewayTest {

    @Test
    void phoneNormalizeMaskAndE164() {
        assertThat(PhoneNumbers.normalize(null)).isNull();
        assertThat(PhoneNumbers.normalize("   ")).isNull();
        assertThat(PhoneNumbers.normalize("0712345678")).isEqualTo("255712345678");
        assertThat(PhoneNumbers.normalize("+255712345678")).isEqualTo("255712345678");
        assertThat(PhoneNumbers.normalize("255712345678")).isEqualTo("255712345678");
        assertThat(PhoneNumbers.normalize("753493500")).isEqualTo("255753493500");
        assertThat(PhoneNumbers.normalize("12345")).isEqualTo("12345");
        assertThat(PhoneNumbers.persist(null)).isNull();
        assertThat(PhoneNumbers.persist("   ")).isNull();
        assertThat(PhoneNumbers.persist("0753493500")).isEqualTo("255753493500");
        assertThat(PhoneNumbers.persist("255753493500")).isEqualTo("255753493500");
        assertThat(PhoneNumbers.toE164Like(null)).isNull();
        assertThat(PhoneNumbers.toE164Like("   ")).isNull();
        assertThat(PhoneNumbers.toE164Like("abc")).isNull();
        assertThat(PhoneNumbers.toE164Like("+++")).isEqualTo("+++");
        assertThat(PhoneNumbers.toE164Like("0756628215")).isEqualTo("+255756628215");
        assertThat(PhoneNumbers.toE164Like("255756628215")).isEqualTo("+255756628215");
        assertThat(PhoneNumbers.toE164Like("+255756628215")).isEqualTo("+255756628215");
        assertThat(PhoneNumbers.toE164Like("756628215")).isEqualTo("+255756628215");
        assertThat(PhoneNumbers.toE164Like("12345")).isEqualTo("+12345");
        assertThat(PhoneNumbers.mask(null)).isNull();
        assertThat(PhoneNumbers.mask("123")).isEqualTo("123");
        assertThat(PhoneNumbers.mask("255712345678")).isEqualTo("2557****678");
        assertThat(PhoneNumbers.mask("+255712345678")).isEqualTo("+255****678");
    }

    @Test
    void loggingGatewaySends() {
        SmsSendResult result = new LoggingSmsGateway().send("255712345678", "HALO", "Hello");
        assertThat(result.sent()).isTrue();
        assertThat(result.providerRef()).isEqualTo("log");
        assertThat(SmsSendResult.failed("x").sent()).isFalse();
        assertThat(SmsSendResult.skipped("off").providerRef()).isEqualTo("skipped");
    }

    @Test
    void notificationGatewayValidatesAndQueues() {
        RestClient unusedClient = RestClient.builder().build();
        SmsProperties.Messaging noUrl = SmsProperties.Messaging.of(true, "log-sms", "HALO", " ", "key");
        assertThat(new NotificationServiceSmsGateway(noUrl, unusedClient).send("0756", "HALO", "Hi").error())
                .contains("not configured");
        SmsProperties.Messaging nullUrl = SmsProperties.Messaging.of(true, "log-sms", "HALO", null, "key");
        assertThat(new NotificationServiceSmsGateway(nullUrl, unusedClient).send("0756", "HALO", "Hi").error())
                .contains("not configured");

        SmsProperties.Messaging noKey = SmsProperties.Messaging.of(
                true, "log-sms", "HALO", "http://localhost:7575", "  ");
        assertThat(new NotificationServiceSmsGateway(noKey, unusedClient)
                .send("0756628215", "HALO", "Hi").error())
                .isEqualTo("api-key empty");
        assertThat(new NotificationServiceSmsGateway(noKey, unusedClient).send(" ", "HALO", "Hi").error())
                .isEqualTo("no-phone");
        assertThat(new NotificationServiceSmsGateway(noKey, unusedClient)
                .send("0756628215", "HALO", "  ").error())
                .isEqualTo("empty-body");
        assertThat(new NotificationServiceSmsGateway(noKey, unusedClient)
                .send("0756628215", "HALO", null).error())
                .isEqualTo("empty-body");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        server.expect(requestTo("http://localhost:7575/f1/queueNotification"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-Key", "secret"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("SMS"))
                .andExpect(jsonPath("$.to").value("+255756628215"))
                .andExpect(jsonPath("$.body").value("Fee reminder"))
                .andExpect(jsonPath("$.senderName").value("HALO"))
                .andExpect(jsonPath("$.provider").value("log-sms"))
                .andExpect(jsonPath("$.tag").value("STUDENT_MESSAGE"))
                .andRespond(withSuccess("{\"error\":false,\"errorMessage\":\"queued\",\"data\":1}", MediaType.APPLICATION_JSON));
        SmsProperties.Messaging messaging = new SmsProperties.Messaging(
                true, "log-sms", "HALO", "http://localhost:7575/", "secret", "f1/queueNotification");
        assertThat(new NotificationServiceSmsGateway(messaging, client)
                .send("0756628215", null, "Fee reminder").sent()).isTrue();
        server.verify();
    }

    @Test
    void notificationGatewayCoversRejectHttpAndErrors() {
        RestClient.Builder rejected = RestClient.builder();
        MockRestServiceServer rejectedServer = MockRestServiceServer.bindTo(rejected).build();
        rejectedServer.expect(requestTo("http://localhost:7575/f1/queueNotification"))
                .andRespond(withSuccess("{\"error\":true,\"errorMessage\":\"quota\"}", MediaType.APPLICATION_JSON));
        SmsProperties.Messaging messaging = SmsProperties.Messaging.of(
                true, "", "HALO", "http://localhost:7575", "secret");
        assertThat(new NotificationServiceSmsGateway(messaging, rejected.build())
                .send("255756628215", "  ", "Hi").error())
                .contains("quota");
        rejectedServer.verify();

        RestClient.Builder emptyBody = RestClient.builder();
        MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(emptyBody).build();
        emptyServer.expect(requestTo("http://localhost:7575/f1/queueNotification"))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));
        assertThat(new NotificationServiceSmsGateway(messaging, emptyBody.build())
                .send("+255756628215", "HALO", "Hi").error())
                .contains("empty body");
        emptyServer.verify();

        RestClient.Builder trueNoMessage = RestClient.builder();
        MockRestServiceServer trueNoMessageServer = MockRestServiceServer.bindTo(trueNoMessage).build();
        trueNoMessageServer.expect(requestTo("http://localhost:7575/f1/queueNotification"))
                .andRespond(withSuccess("{\"error\":true}", MediaType.APPLICATION_JSON));
        assertThat(new NotificationServiceSmsGateway(messaging, trueNoMessage.build())
                .send("0756628215", "HALO", "Hi").error())
                .isEqualTo("Notification service: ");
        trueNoMessageServer.verify();

        RestClient.Builder nullError = RestClient.builder();
        MockRestServiceServer nullErrorServer = MockRestServiceServer.bindTo(nullError).build();
        nullErrorServer.expect(requestTo("http://localhost:7575/f1/queueNotification"))
                .andRespond(withSuccess("{\"errorMessage\":\"missing flag\"}", MediaType.APPLICATION_JSON));
        assertThat(new NotificationServiceSmsGateway(messaging, nullError.build())
                .send("756628215", "HALO", "Hi").error())
                .contains("missing flag");
        nullErrorServer.verify();

        RestClient.Builder httpError = RestClient.builder();
        MockRestServiceServer httpServer = MockRestServiceServer.bindTo(httpError).build();
        httpServer.expect(requestTo("http://localhost:7575/f1/queueNotification"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("x".repeat(500)));
        assertThat(new NotificationServiceSmsGateway(messaging, httpError.build())
                .send("0756628215", "HALO", "Hi").error())
                .contains("HTTP 500");
        httpServer.verify();

        RestClient.Builder badJson = RestClient.builder();
        MockRestServiceServer badJsonServer = MockRestServiceServer.bindTo(badJson).build();
        badJsonServer.expect(requestTo("http://localhost:7575/f1/queueNotification"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        assertThat(new NotificationServiceSmsGateway(messaging, badJson.build())
                .send("0756628215", "HALO", "Hi").error())
                .contains(":");
        badJsonServer.verify();

        SmsProperties.Messaging unreachable = SmsProperties.Messaging.of(
                true, "beem", "HALO", "http://127.0.0.1:1", "secret");
        assertThat(new NotificationServiceSmsGateway(unreachable, RestClient.builder().build())
                .send("0756628215", "HALO", "Hi").error())
                .contains("Cannot reach notification service");

        SmsGatewayConfig config = new SmsGatewayConfig();
        assertThat(config.smsGateway(Fixtures.properties(), RestClient.builder()))
                .isInstanceOf(LoggingSmsGateway.class);
        SmsProperties queued = new SmsProperties(
                Fixtures.properties().jwt(),
                Fixtures.properties().cors(),
                Fixtures.properties().superAdmin(),
                Fixtures.properties().passwordReset(),
                Fixtures.properties().tenancy(),
                SmsProperties.Messaging.of(true, "log-sms", "HALO", "http://localhost:7575", "secret"));
        SmsProperties enabledBare = new SmsProperties(
                Fixtures.properties().jwt(),
                Fixtures.properties().cors(),
                Fixtures.properties().superAdmin(),
                Fixtures.properties().passwordReset(),
                Fixtures.properties().tenancy(),
                SmsProperties.Messaging.of(true, "", "HALO", "", "key"));
        assertThat(config.smsGateway(enabledBare, RestClient.builder()))
                .isInstanceOf(NotificationServiceSmsGateway.class);
    }
}
