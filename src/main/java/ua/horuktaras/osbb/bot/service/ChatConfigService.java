package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.horuktaras.osbb.bot.model.entity.ChatConfig;
import ua.horuktaras.osbb.bot.repository.ChatConfigRepository;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatConfigService {

    private static final Logger log = LoggerFactory.getLogger(ChatConfigService.class);

    private final ChatConfigRepository chatConfigRepository;
    private final ConcurrentHashMap<Long, ChatConfig> cache = new ConcurrentHashMap<>();

    public ChatConfigService(ChatConfigRepository chatConfigRepository) {
        this.chatConfigRepository = chatConfigRepository;
    }

    @Transactional
    public ChatConfig getOrCreate(Long chatId) {
        ChatConfig cached = cache.get(chatId);
        if (cached != null) {
            return cached;
        }
        ChatConfig config = chatConfigRepository.findByChatId(chatId)
                .orElseGet(() -> {
                    log.info("Creating default chat config for chatId={}", chatId);
                    ChatConfig newConfig = new ChatConfig();
                    newConfig.setChatId(chatId);
                    return chatConfigRepository.save(newConfig);
                });
        cache.put(chatId, config);
        return config;
    }

    @Transactional
    public ChatConfig update(Long chatId, ChatConfig updatedConfig) {
        ChatConfig existing = chatConfigRepository.findByChatId(chatId)
                .orElseGet(() -> {
                    ChatConfig c = new ChatConfig();
                    c.setChatId(chatId);
                    return c;
                });

        existing.setVerificationEnabled(updatedConfig.isVerificationEnabled());
        existing.setVerificationTimeoutSeconds(updatedConfig.getVerificationTimeoutSeconds());
        existing.setAntiLinkEnabled(updatedConfig.isAntiLinkEnabled());
        existing.setAntiSpamEnabled(updatedConfig.isAntiSpamEnabled());
        existing.setMaxMessagesPerWindow(updatedConfig.getMaxMessagesPerWindow());
        existing.setRateLimitWindowSeconds(updatedConfig.getRateLimitWindowSeconds());
        existing.setWarningLimit(updatedConfig.getWarningLimit());
        existing.setWarningPunishment(updatedConfig.getWarningPunishment());
        existing.setWarningPunishmentDurationSeconds(updatedConfig.getWarningPunishmentDurationSeconds());
        existing.setAllowedDomains(updatedConfig.getAllowedDomains());
        existing.setBlacklistedKeywords(updatedConfig.getBlacklistedKeywords());

        ChatConfig saved = chatConfigRepository.save(existing);
        cache.put(chatId, saved);
        return saved;
    }

    public void evictCache(Long chatId) {
        cache.remove(chatId);
    }
}
