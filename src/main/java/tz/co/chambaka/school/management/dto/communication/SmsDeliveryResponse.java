package tz.co.chambaka.school.management.dto.communication;

import tz.co.chambaka.school.management.model.enums.SmsDeliveryStatus;

public record SmsDeliveryResponse(
        Long parentId,
        String parentName,
        String phoneMasked,
        SmsDeliveryStatus status,
        String error
) {
}
