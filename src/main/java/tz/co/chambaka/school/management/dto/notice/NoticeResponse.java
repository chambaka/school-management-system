package tz.co.chambaka.school.management.dto.notice;

import tz.co.chambaka.school.management.model.enums.NoticeAudience;

import java.time.Instant;

public record NoticeResponse(
        Long id,
        String title,
        String content,
        NoticeAudience audience,
        Long schoolClassId,
        String schoolClassName,
        boolean published,
        Instant publishAt,
        Instant expiresAt,
        String createdByName
) {
}
