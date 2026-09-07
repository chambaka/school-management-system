package tz.co.chambaka.school.management.sms;

public interface SmsGateway {

    SmsSendResult send(String phone, String senderId, String body);
}
