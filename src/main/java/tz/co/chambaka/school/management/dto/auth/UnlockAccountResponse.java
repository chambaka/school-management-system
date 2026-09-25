package tz.co.chambaka.school.management.dto.auth;

import java.time.Instant;

public record UnlockAccountResponse(
        Long userId,
        String name,
        String email,
        Instant lockedUntil
) {
}
