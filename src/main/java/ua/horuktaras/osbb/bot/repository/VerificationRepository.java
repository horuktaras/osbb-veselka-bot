package ua.horuktaras.osbb.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ua.horuktaras.osbb.bot.model.entity.Verification;
import ua.horuktaras.osbb.bot.model.enums.VerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {

    Optional<Verification> findByChatIdAndTelegramUserIdAndStatus(
            Long chatId, Long telegramUserId, VerificationStatus status);

    List<Verification> findAllByStatusAndExpiresAtBefore(VerificationStatus status, Instant now);
}
