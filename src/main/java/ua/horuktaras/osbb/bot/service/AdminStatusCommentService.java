package ua.horuktaras.osbb.bot.service;

import org.springframework.stereotype.Service;
import ua.horuktaras.osbb.bot.model.enums.RequestStatus;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks in-memory state for admin status changes that require a comment prompt.
 */
@Service
public class AdminStatusCommentService {

    private final ConcurrentHashMap<Long, PendingStatusChange> pending = new ConcurrentHashMap<>();

    public record PendingStatusChange(
            Long requestId,
            RequestStatus newStatus,
            Long chatId,
            Integer messageId,
            boolean awaitingText
    ) {
        public PendingStatusChange withAwaitingText() {
            return new PendingStatusChange(requestId, newStatus, chatId, messageId, true);
        }
    }

    public void store(Long adminId, Long requestId, RequestStatus newStatus, Long chatId, Integer messageId) {
        pending.put(adminId, new PendingStatusChange(requestId, newStatus, chatId, messageId, false));
    }

    public Optional<PendingStatusChange> get(Long adminId) {
        return Optional.ofNullable(pending.get(adminId));
    }

    public void setAwaitingText(Long adminId) {
        pending.computeIfPresent(adminId, (id, p) -> p.withAwaitingText());
    }

    public boolean isAwaitingText(Long adminId) {
        PendingStatusChange p = pending.get(adminId);
        return p != null && p.awaitingText();
    }

    public void remove(Long adminId) {
        pending.remove(adminId);
    }
}
