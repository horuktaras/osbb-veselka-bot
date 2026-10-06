package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.horuktaras.osbb.bot.model.entity.Verification;
import ua.horuktaras.osbb.bot.model.enums.VerificationStatus;
import ua.horuktaras.osbb.bot.repository.VerificationRepository;

import java.time.Instant;
import java.util.Optional;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final VerificationRepository verificationRepository;

    public VerificationService(VerificationRepository verificationRepository) {
        this.verificationRepository = verificationRepository;
    }

    @Transactional
    public Verification startVerification(Long chatId, Long telegramUserId, int timeoutSeconds) {
        // Cancel any existing pending verification first
        verificationRepository.findByChatIdAndTelegramUserIdAndStatus(
                        chatId, telegramUserId, VerificationStatus.PENDING)
                .ifPresent(existing -> {
                    existing.setStatus(VerificationStatus.EXPIRED);
                    existing.setCompletedAt(Instant.now());
                    verificationRepository.save(existing);
                });

        Verification verification = new Verification();
        verification.setChatId(chatId);
        verification.setTelegramUserId(telegramUserId);
        verification.setStatus(VerificationStatus.PENDING);
        verification.setCreatedAt(Instant.now());
        verification.setExpiresAt(Instant.now().plusSeconds(timeoutSeconds));

        Verification saved = verificationRepository.save(verification);
        log.info("Verification started for userId={} in chatId={}, expires={}", telegramUserId, chatId, saved.getExpiresAt());
        return saved;
    }

    @Transactional
    public boolean completeVerification(Long chatId, Long telegramUserId, Long requestingUserId) {
        if (!telegramUserId.equals(requestingUserId)) {
            log.warn("Verification attempt by wrong user: expected={}, got={}", telegramUserId, requestingUserId);
            return false;
        }

        Optional<Verification> opt = verificationRepository.findByChatIdAndTelegramUserIdAndStatus(
                chatId, telegramUserId, VerificationStatus.PENDING);

        if (opt.isEmpty()) {
            // Check if already verified (idempotent)
            log.debug("No pending verification found for userId={} in chatId={}", telegramUserId, chatId);
            return false;
        }

        Verification verification = opt.get();

        if (Instant.now().isAfter(verification.getExpiresAt())) {
            verification.setStatus(VerificationStatus.EXPIRED);
            verification.setCompletedAt(Instant.now());
            verificationRepository.save(verification);
            log.info("Verification expired for userId={} in chatId={}", telegramUserId, chatId);
            return false;
        }

        verification.setStatus(VerificationStatus.VERIFIED);
        verification.setCompletedAt(Instant.now());
        verificationRepository.save(verification);
        log.info("Verification completed for userId={} in chatId={}", telegramUserId, chatId);
        return true;
    }

    @Transactional
    public void expireVerification(Verification v) {
        v.setStatus(VerificationStatus.EXPIRED);
        v.setCompletedAt(Instant.now());
        verificationRepository.save(v);
        log.info("Verification expired for userId={} in chatId={}", v.getTelegramUserId(), v.getChatId());
    }

    @Transactional(readOnly = true)
    public Optional<Verification> getPendingVerification(Long chatId, Long telegramUserId) {
        return verificationRepository.findByChatIdAndTelegramUserIdAndStatus(
                chatId, telegramUserId, VerificationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public java.util.List<Verification> findExpiredPendingVerifications() {
        return verificationRepository.findAllByStatusAndExpiresAtBefore(
                VerificationStatus.PENDING, Instant.now());
    }

    @Transactional
    public void setVerificationMessageId(Long verificationId, Long messageId) {
        verificationRepository.findById(verificationId).ifPresent(v -> {
            v.setVerificationMessageId(messageId);
            verificationRepository.save(v);
        });
    }
}
