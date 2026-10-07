package ua.horuktaras.osbb.bot.service;

import org.springframework.stereotype.Service;
import ua.horuktaras.osbb.bot.model.dto.RequestDraft;
import ua.horuktaras.osbb.bot.model.enums.ConversationStep;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {

    private final ConcurrentHashMap<Long, RequestDraft> drafts = new ConcurrentHashMap<>();

    public RequestDraft startDraft(Long userId, Long sourceChatId, String username) {
        RequestDraft draft = new RequestDraft();
        draft.setUserId(userId);
        draft.setSourceChatId(sourceChatId);
        draft.setTelegramUsername(username);
        draft.setStep(ConversationStep.AWAITING_NAME);
        drafts.put(userId, draft);
        return draft;
    }

    public Optional<RequestDraft> getDraft(Long userId) {
        return Optional.ofNullable(drafts.get(userId));
    }

    public void removeDraft(Long userId) {
        drafts.remove(userId);
    }

    public boolean hasDraft(Long userId) {
        return drafts.containsKey(userId);
    }
}
