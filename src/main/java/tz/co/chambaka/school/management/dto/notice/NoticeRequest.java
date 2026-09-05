package tz.co.chambaka.school.management.dto.notice;

import tz.co.chambaka.school.management.model.enums.NoticeAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record NoticeRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotNull NoticeAudience audience,
        Long schoolClassId,
        boolean published,
        Instant publishAt,
        Instant expiresAt
) {
}
