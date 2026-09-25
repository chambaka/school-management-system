package tz.co.chambaka.school.management.repository;

import tz.co.chambaka.school.management.model.TwoFactorChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TwoFactorChallengeRepository extends JpaRepository<TwoFactorChallenge, Long> {

    Optional<TwoFactorChallenge> findByTokenHashAndConsumedFalse(String tokenHash);

    void deleteByUserId(Long userId);
}
