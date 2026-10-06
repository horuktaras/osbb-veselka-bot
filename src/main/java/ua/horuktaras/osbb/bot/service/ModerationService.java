package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.dto.ModerationResult;
import ua.horuktaras.osbb.bot.model.entity.ChatConfig;
import ua.horuktaras.osbb.bot.model.enums.ModerationAction;
import ua.horuktaras.osbb.bot.model.enums.WarningPunishment;
import ua.horuktaras.osbb.bot.util.MessageFormatter;

import java.time.Duration;
import java.time.Instant;

@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);

    private final ChatMemberService chatMemberService;
    private final ChatConfigService chatConfigService;
    private final ModerationLogService moderationLogService;
    private final TelegramApiService telegramApiService;

    public ModerationService(
            ChatMemberService chatMemberService,
            ChatConfigService chatConfigService,
            ModerationLogService moderationLogService,
            TelegramApiService telegramApiService
    ) {
        this.chatMemberService = chatMemberService;
        this.chatConfigService = chatConfigService;
        this.moderationLogService = moderationLogService;
        this.telegramApiService = telegramApiService;
    }

    public ModerationResult warnUser(Long chatId, Long adminId, Long targetId, String reason, TelegramClient client) {
        ChatConfig config = chatConfigService.getOrCreate(chatId);
        int newCount = chatMemberService.addWarning(chatId, targetId);
        moderationLogService.log(chatId, adminId, targetId, ModerationAction.WARN, reason,
                "warningCount=" + newCount);

        if (newCount >= config.getWarningLimit()) {
            if (config.getWarningPunishment() == WarningPunishment.BAN) {
                boolean banned = telegramApiService.banUser(chatId, targetId, null);
                if (banned) {
                    chatMemberService.ban(chatId, targetId);
                    moderationLogService.log(chatId, adminId, targetId, ModerationAction.AUTO_BAN,
                            "Auto-ban after " + newCount + " warnings", null);
                    return ModerationResult.success(ModerationAction.WARN,
                            MessageFormatter.formatWarningMessage(newCount, config.getWarningLimit())
                                    + "\nUser has been banned.");
                }
            } else {
                Duration duration = Duration.ofSeconds(config.getWarningPunishmentDurationSeconds());
                Instant until = Instant.now().plus(duration);
                ChatPermissions noPermissions = ChatPermissions.builder()
                        .canSendMessages(false)
                        .canSendAudios(false)
                        .canSendDocuments(false)
                        .canSendPhotos(false)
                        .canSendVideos(false)
                        .canSendVideoNotes(false)
                        .canSendVoiceNotes(false)
                        .canSendOtherMessages(false)
                        .canAddWebPagePreviews(false)
                        .build();
                boolean muted = telegramApiService.restrictUser(chatId, targetId, noPermissions);
                if (muted) {
                    chatMemberService.mute(chatId, targetId, until);
                    moderationLogService.log(chatId, adminId, targetId, ModerationAction.SPAM_MUTED,
                            "Auto-mute after " + newCount + " warnings",
                            "duration=" + config.getWarningPunishmentDurationSeconds() + "s");
                    return ModerationResult.success(ModerationAction.WARN,
                            MessageFormatter.formatWarningMessage(newCount, config.getWarningLimit())
                                    + "\nUser has been muted for " + formatDuration(duration) + ".");
                }
            }
        }

        return ModerationResult.success(ModerationAction.WARN,
                MessageFormatter.formatWarningMessage(newCount, config.getWarningLimit()));
    }

    public ModerationResult muteUser(Long chatId, Long adminId, Long targetId, Duration duration, String reason, TelegramClient client) {
        ChatPermissions noPermissions = ChatPermissions.builder()
                .canSendMessages(false)
                .canSendAudios(false)
                .canSendDocuments(false)
                .canSendPhotos(false)
                .canSendVideos(false)
                .canSendVideoNotes(false)
                .canSendVoiceNotes(false)
                .canSendOtherMessages(false)
                .canAddWebPagePreviews(false)
                .build();
        boolean success = telegramApiService.restrictUser(chatId, targetId, noPermissions);
        if (!success) {
            return ModerationResult.failure(ModerationAction.MUTE, "Failed to mute user via Telegram API.");
        }
        Instant until = duration != null && !duration.isZero()
                ? Instant.now().plus(duration) : null;
        chatMemberService.mute(chatId, targetId, until);
        moderationLogService.log(chatId, adminId, targetId, ModerationAction.MUTE, reason,
                duration != null ? "duration=" + duration.getSeconds() + "s" : "permanent");
        return ModerationResult.success(ModerationAction.MUTE,
                MessageFormatter.formatMuteMessage("user", duration != null ? duration : Duration.ZERO));
    }

    public ModerationResult unmuteUser(Long chatId, Long adminId, Long targetId, String reason, TelegramClient client) {
        boolean success = telegramApiService.unrestrictUser(chatId, targetId);
        if (!success) {
            return ModerationResult.failure(ModerationAction.UNMUTE, "Failed to unmute user via Telegram API.");
        }
        chatMemberService.unmute(chatId, targetId);
        moderationLogService.log(chatId, adminId, targetId, ModerationAction.UNMUTE, reason, null);
        return ModerationResult.success(ModerationAction.UNMUTE, "User has been unmuted.");
    }

    public ModerationResult banUser(Long chatId, Long adminId, Long targetId, String reason, TelegramClient client) {
        boolean success = telegramApiService.banUser(chatId, targetId, null);
        if (!success) {
            return ModerationResult.failure(ModerationAction.BAN, "Failed to ban user via Telegram API.");
        }
        chatMemberService.ban(chatId, targetId);
        moderationLogService.log(chatId, adminId, targetId, ModerationAction.BAN, reason, null);
        return ModerationResult.success(ModerationAction.BAN, "User has been banned.");
    }

    public ModerationResult unbanUser(Long chatId, Long adminId, Long targetId, String reason, TelegramClient client) {
        boolean success = telegramApiService.unbanUser(chatId, targetId);
        if (!success) {
            return ModerationResult.failure(ModerationAction.UNBAN, "Failed to unban user via Telegram API.");
        }
        chatMemberService.unban(chatId, targetId);
        moderationLogService.log(chatId, adminId, targetId, ModerationAction.UNBAN, reason, null);
        return ModerationResult.success(ModerationAction.UNBAN, "User has been unbanned.");
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        if (days > 0) return days + " day(s)";
        long hours = duration.toHours();
        if (hours > 0) return hours + " hour(s)";
        long minutes = duration.toMinutes();
        if (minutes > 0) return minutes + " minute(s)";
        return duration.getSeconds() + " second(s)";
    }
}
