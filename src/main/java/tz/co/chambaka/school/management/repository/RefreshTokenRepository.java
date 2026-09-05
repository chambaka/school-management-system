package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    void deleteByUserId(Long userId);
}
