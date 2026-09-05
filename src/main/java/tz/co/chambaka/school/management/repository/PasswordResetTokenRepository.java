package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.PasswordResetToken;
import tz.co.chambaka.school.management.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByUserAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(User user, Instant now);

    Optional<PasswordResetToken> findBySessionHashAndConsumedFalse(String sessionHash);

    void deleteByUserId(Long userId);
}
