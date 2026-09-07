package tz.co.chambaka.school.management.sms;

import tz.co.chambaka.school.management.config.SmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SmsGatewayConfig {

    @Bean
    public SmsGateway smsGateway(SmsProperties properties, RestClient.Builder restClientBuilder) {
        SmsProperties.Messaging messaging = properties.messaging();
        if (messaging.active()) {
            return new NotificationServiceSmsGateway(messaging, restClientBuilder.build());
        }
        return new LoggingSmsGateway();
    }
}
