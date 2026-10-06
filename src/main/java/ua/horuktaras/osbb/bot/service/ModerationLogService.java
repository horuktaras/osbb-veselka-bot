package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.horuktaras.osbb.bot.model.entity.ModerationLog;
import ua.horuktaras.osbb.bot.model.enums.ModerationAction;
import ua.horuktaras.osbb.bot.repository.ModerationLogRepository;

import java.util.List;

@Service
public class ModerationLogService {

    private static final Logger log = LoggerFactory.getLogger(ModerationLogService.class);

    private final ModerationLogRepository moderationLogRepository;

    public ModerationLogService(ModerationLogRepository moderationLogRepository) {
        this.moderationLogRepository = moderationLogRepository;
    }

    @Transactional
    public void log(Long chatId, Long adminId, Long targetId, ModerationAction action, String reason, String metadata) {
        ModerationLog entry = new ModerationLog();
        entry.setChatId(chatId);
        entry.setAdminTelegramUserId(adminId);
        entry.setTargetTelegramUserId(targetId);
        entry.setAction(action);
        entry.setReason(reason);
        entry.setMetadata(metadata);
        moderationLogRepository.save(entry);
        log.info("Moderation log: chatId={} admin={} target={} action={} reason={}",
                chatId, adminId, targetId, action, reason);
    }

    @Transactional(readOnly = true)
    public List<ModerationLog> getRecentActions(Long chatId, int limit) {
        return moderationLogRepository
                .findByChatIdOrderByCreatedAtDesc(chatId, PageRequest.of(0, limit))
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<ModerationLog> getUserActions(Long chatId, Long userId, int limit) {
        return moderationLogRepository
                .findByChatIdAndTargetTelegramUserIdOrderByCreatedAtDesc(chatId, userId, PageRequest.of(0, limit))
                .getContent();
    }
}
