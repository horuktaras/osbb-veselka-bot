package ua.horuktaras.osbb.bot.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FloodControlTest {

    private FloodControl floodControl;

    @BeforeEach
    void setUp() {
        floodControl = new FloodControl();
    }

    @Test
    void notRateLimitedBelowThreshold() {
        Long chatId = 100L;
        Long userId = 1L;
        int maxMessages = 5;
        int windowSeconds = 60;

        for (int i = 0; i < maxMessages - 1; i++) {
            floodControl.recordMessage(chatId, userId);
        }

        assertFalse(floodControl.isRateLimited(chatId, userId, maxMessages, windowSeconds));
    }

    @Test
    void rateLimitedAfterThresholdExceeded() {
        Long chatId = 100L;
        Long userId = 2L;
        int maxMessages = 3;
        int windowSeconds = 60;

        for (int i = 0; i < maxMessages; i++) {
            floodControl.recordMessage(chatId, userId);
        }

        assertTrue(floodControl.isRateLimited(chatId, userId, maxMessages, windowSeconds));
    }

    @Test
    void differentUsersAreIndependent() {
        Long chatId = 100L;
        Long user1 = 10L;
        Long user2 = 20L;
        int maxMessages = 3;
        int windowSeconds = 60;

        for (int i = 0; i < maxMessages; i++) {
            floodControl.recordMessage(chatId, user1);
        }

        // User1 is rate limited
        assertTrue(floodControl.isRateLimited(chatId, user1, maxMessages, windowSeconds));
        // User2 is not rate limited
        assertFalse(floodControl.isRateLimited(chatId, user2, maxMessages, windowSeconds));
    }

    @Test
    void differentChatsAreIndependent() {
        Long chat1 = 100L;
        Long chat2 = 200L;
        Long userId = 1L;
        int maxMessages = 3;
        int windowSeconds = 60;

        for (int i = 0; i < maxMessages; i++) {
            floodControl.recordMessage(chat1, userId);
        }

        // User in chat1 is rate limited
        assertTrue(floodControl.isRateLimited(chat1, userId, maxMessages, windowSeconds));
        // User in chat2 is not rate limited
        assertFalse(floodControl.isRateLimited(chat2, userId, maxMessages, windowSeconds));
    }

    @Test
    void cleanupExpiredDoesNotThrow() {
        Long chatId = 100L;
        Long userId = 1L;
        floodControl.recordMessage(chatId, userId);

        assertDoesNotThrow(() -> floodControl.cleanupExpired());
    }

    @Test
    void zeroRecordsNotRateLimited() {
        assertFalse(floodControl.isRateLimited(999L, 999L, 5, 60));
    }
}
