package ua.horuktaras.osbb.bot.moderation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FloodControl {

    private static final Logger log = LoggerFactory.getLogger(FloodControl.class);

    // Key: "chatId:userId" -> deque of message timestamps (epoch millis)
    private final ConcurrentHashMap<String, Deque<Long>> messageTimestamps = new ConcurrentHashMap<>();

    /**
     * Checks whether a user in a chat exceeds the rate limit.
     *
     * @param chatId         the chat id
     * @param userId         the user id
     * @param maxMessages    max messages allowed in window
     * @param windowSeconds  length of the sliding window in seconds
     * @return true if rate limited
     */
    public boolean isRateLimited(Long chatId, Long userId, int maxMessages, int windowSeconds) {
        String key = buildKey(chatId, userId);
        long windowMs = (long) windowSeconds * 1000;
        long cutoff = Instant.now().toEpochMilli() - windowMs;

        Deque<Long> timestamps = messageTimestamps.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            // Remove expired timestamps
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            return timestamps.size() >= maxMessages;
        }
    }

    /**
     * Records a message send event for a user.
     *
     * @param chatId the chat id
     * @param userId the user id
     */
    public void recordMessage(Long chatId, Long userId) {
        String key = buildKey(chatId, userId);
        Deque<Long> timestamps = messageTimestamps.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            timestamps.addLast(Instant.now().toEpochMilli());
        }
    }

    /**
     * Cleans up expired entries to prevent memory leaks.
     * Called by scheduler.
     */
    public void cleanupExpired() {
        long cutoff = Instant.now().toEpochMilli() - 3600_000L; // 1 hour
        int removedKeys = 0;

        Iterator<java.util.Map.Entry<String, Deque<Long>>> it = messageTimestamps.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<String, Deque<Long>> entry = it.next();
            Deque<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                    timestamps.pollFirst();
                }
                if (timestamps.isEmpty()) {
                    it.remove();
                    removedKeys++;
                }
            }
        }

        if (removedKeys > 0) {
            log.debug("FloodControl cleanup: removed {} empty entries", removedKeys);
        }
    }

    private String buildKey(Long chatId, Long userId) {
        return chatId + ":" + userId;
    }
}
