package tz.co.chambaka.school.management.sms;

public record SmsSendResult(boolean sent, String providerRef, String error) {

    public static SmsSendResult ok(String providerRef) {
        return new SmsSendResult(true, providerRef, null);
    }

    public static SmsSendResult failed(String error) {
        return new SmsSendResult(false, null, error);
    }

    public static SmsSendResult skipped(String reason) {
        return new SmsSendResult(false, "skipped", reason);
    }
}
