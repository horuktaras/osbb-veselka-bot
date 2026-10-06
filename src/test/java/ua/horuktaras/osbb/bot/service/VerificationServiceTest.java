package ua.horuktaras.osbb.bot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.horuktaras.osbb.bot.model.entity.Verification;
import ua.horuktaras.osbb.bot.model.enums.VerificationStatus;
import ua.horuktaras.osbb.bot.repository.VerificationRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationRepository verificationRepository;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(verificationRepository);
    }

    @Test
    void startVerificationCreatesPendingRecord() {
        Long chatId = 100L;
        Long userId = 1L;

        when(verificationRepository.findByChatIdAndTelegramUserIdAndStatus(chatId, userId, VerificationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(verificationRepository.save(any(Verification.class))).thenAnswer(invocation -> {
            Verification v = invocation.getArgument(0);
            v.setId(1L);
            return v;
        });

        Verification result = verificationService.startVerification(chatId, userId, 600);

        assertNotNull(result);
        assertEquals(VerificationStatus.PENDING, result.getStatus());
        assertEquals(chatId, result.getChatId());
        assertEquals(userId, result.getTelegramUserId());
        assertNotNull(result.getExpiresAt());
        assertTrue(result.getExpiresAt().isAfter(Instant.now()));
        verify(verificationRepository).save(any(Verification.class));
    }

    @Test
    void completeVerificationSetsVerified() {
        Long chatId = 100L;
        Long userId = 1L;

        Verification pending = new Verification();
        pending.setId(1L);
        pending.setChatId(chatId);
        pending.setTelegramUserId(userId);
        pending.setStatus(VerificationStatus.PENDING);
        pending.setCreatedAt(Instant.now());
        pending.setExpiresAt(Instant.now().plusSeconds(600));

        when(verificationRepository.findByChatIdAndTelegramUserIdAndStatus(chatId, userId, VerificationStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(verificationRepository.save(any(Verification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = verificationService.completeVerification(chatId, userId, userId);

        assertTrue(result);
        assertEquals(VerificationStatus.VERIFIED, pending.getStatus());
        assertNotNull(pending.getCompletedAt());
        verify(verificationRepository).save(pending);
    }

    @Test
    void completeVerificationByWrongUserReturnsFalse() {
        Long chatId = 100L;
        Long userId = 1L;
        Long wrongUserId = 999L;

        boolean result = verificationService.completeVerification(chatId, userId, wrongUserId);

        assertFalse(result);
        verify(verificationRepository, never()).findByChatIdAndTelegramUserIdAndStatus(any(), any(), any());
    }

    @Test
    void completeVerificationWhenNoPendingReturnsFalse() {
        Long chatId = 100L;
        Long userId = 1L;

        when(verificationRepository.findByChatIdAndTelegramUserIdAndStatus(chatId, userId, VerificationStatus.PENDING))
                .thenReturn(Optional.empty());

        boolean result = verificationService.completeVerification(chatId, userId, userId);

        assertFalse(result);
    }

    @Test
    void completeVerificationWhenExpiredReturnsFalse() {
        Long chatId = 100L;
        Long userId = 1L;

        Verification expired = new Verification();
        expired.setId(1L);
        expired.setChatId(chatId);
        expired.setTelegramUserId(userId);
        expired.setStatus(VerificationStatus.PENDING);
        expired.setCreatedAt(Instant.now().minusSeconds(1200));
        expired.setExpiresAt(Instant.now().minusSeconds(600)); // already expired

        when(verificationRepository.findByChatIdAndTelegramUserIdAndStatus(chatId, userId, VerificationStatus.PENDING))
                .thenReturn(Optional.of(expired));
        when(verificationRepository.save(any(Verification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = verificationService.completeVerification(chatId, userId, userId);

        assertFalse(result);
        assertEquals(VerificationStatus.EXPIRED, expired.getStatus());
    }
}
