package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminPermissionService {

    private static final Logger log = LoggerFactory.getLogger(AdminPermissionService.class);
    private static final long CACHE_TTL_SECONDS = 30;

    private record CacheEntry(boolean isAdmin, Instant expiresAt) {}

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public boolean isAdmin(Long chatId, Long userId, TelegramClient client) {
        String key = chatId + ":" + userId;
        CacheEntry entry = cache.get(key);
        if (entry != null && Instant.now().isBefore(entry.expiresAt())) {
            return entry.isAdmin();
        }

        try {
            GetChatAdministrators request = new GetChatAdministrators(String.valueOf(chatId));
            List<ChatMember> admins = client.execute(request);
            boolean adminResult = admins.stream().anyMatch(cm -> {
                if (cm instanceof ChatMemberOwner owner) {
                    return owner.getUser().getId().equals(userId);
                }
                if (cm instanceof ChatMemberAdministrator admin) {
                    return admin.getUser().getId().equals(userId);
                }
                return false;
            });
            cache.put(key, new CacheEntry(adminResult, Instant.now().plusSeconds(CACHE_TTL_SECONDS)));
            return adminResult;
        } catch (TelegramApiException e) {
            log.warn("Failed to get chat administrators for chatId={}: {}", chatId, e.getMessage());
            return false;
        }
    }

    public void evictCache(Long chatId, Long userId) {
        cache.remove(chatId + ":" + userId);
    }

    public void evictChatCache(Long chatId) {
        String prefix = chatId + ":";
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
