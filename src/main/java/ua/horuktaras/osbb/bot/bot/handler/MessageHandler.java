package ua.horuktaras.osbb.bot.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ua.horuktaras.osbb.bot.bot.command.CommandProcessor;
import ua.horuktaras.osbb.bot.model.entity.ChatConfig;
import ua.horuktaras.osbb.bot.model.enums.ModerationAction;
import ua.horuktaras.osbb.bot.moderation.FloodControl;
import ua.horuktaras.osbb.bot.moderation.LinkDetector;
import ua.horuktaras.osbb.bot.service.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);

    private final ChatConfigService chatConfigService;
    private final FloodControl floodControl;
    private final LinkDetector linkDetector;
    private final TelegramApiService telegramApiService;
    private final ModerationLogService moderationLogService;
    private final ChatMemberService chatMemberService;
    private final CommandProcessor commandProcessor;

    public MessageHandler(
            ChatConfigService chatConfigService,
            FloodControl floodControl,
            LinkDetector linkDetector,
            TelegramApiService telegramApiService,
            ModerationLogService moderationLogService,
            ChatMemberService chatMemberService,
            CommandProcessor commandProcessor
    ) {
        this.chatConfigService = chatConfigService;
        this.floodControl = floodControl;
        this.linkDetector = linkDetector;
        this.telegramApiService = telegramApiService;
        this.moderationLogService = moderationLogService;
        this.chatMemberService = chatMemberService;
        this.commandProcessor = commandProcessor;
    }

    public void handle(Update update) {
        Message message = update.getMessage();
        if (message == null || message.getFrom() == null) return;

        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String text = message.getText() != null ? message.getText() : message.getCaption();

        // Commands take priority
        if (text != null && text.startsWith("/")) {
            commandProcessor.process(update);
            return;
        }

        ChatConfig config = chatConfigService.getOrCreate(chatId);

        // Anti-spam check
        if (config.isAntiSpamEnabled()) {
            floodControl.recordMessage(chatId, userId);
            if (floodControl.isRateLimited(chatId, userId,
                    config.getMaxMessagesPerWindow(), config.getRateLimitWindowSeconds())) {
                handleSpamDetected(chatId, userId, message);
                return;
            }
        }

        // Anti-link check
        if (config.isAntiLinkEnabled() && text != null) {
            Set<String> allowedDomains = parseAllowedDomains(config.getAllowedDomains());
            if (linkDetector.containsBlockedLink(text, allowedDomains)) {
                handleLinkDetected(chatId, userId, message);
                return;
            }
        }

        // Check blacklisted keywords
        if (text != null && config.getBlacklistedKeywords() != null && !config.getBlacklistedKeywords().isBlank()) {
            String[] keywords = config.getBlacklistedKeywords().split(",");
            String lowerText = text.toLowerCase();
            for (String kw : keywords) {
                if (!kw.isBlank() && lowerText.contains(kw.trim().toLowerCase())) {
                    log.info("Blacklisted keyword detected in chatId={} userId={}", chatId, userId);
                    telegramApiService.deleteMessage(chatId, message.getMessageId());
                    moderationLogService.log(chatId, null, userId, ModerationAction.LINK_REMOVED,
                            "Blacklisted keyword: " + kw.trim(), null);
                    return;
                }
            }
        }
    }

    private void handleSpamDetected(Long chatId, Long userId, Message message) {
        log.info("Spam detected: chatId={} userId={}", chatId, userId);
        telegramApiService.deleteMessage(chatId, message.getMessageId());

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
        telegramApiService.restrictUser(chatId, userId, noPermissions);
        chatMemberService.mute(chatId, userId, Instant.now().plus(Duration.ofMinutes(10)));

        moderationLogService.log(chatId, null, userId, ModerationAction.SPAM_MUTED,
                "Auto-mute: spam detected", null);

        SendMessage warning = SendMessage.builder()
                .chatId(chatId)
                .text("\uD83D\uDEAB User has been muted for 10 minutes due to spamming.")
                .parseMode("HTML")
                .build();
        telegramApiService.sendMessage(warning);
    }

    private void handleLinkDetected(Long chatId, Long userId, Message message) {
        log.info("Link detected in message from userId={} in chatId={}", userId, chatId);
        telegramApiService.deleteMessage(chatId, message.getMessageId());

        moderationLogService.log(chatId, null, userId, ModerationAction.LINK_REMOVED,
                "Link detected and removed", null);

        SendMessage warning = SendMessage.builder()
                .chatId(chatId)
                .text("\uD83D\uDD17 Link removed. External links are not allowed in this chat.")
                .parseMode("HTML")
                .build();
        telegramApiService.sendMessage(warning);
    }

    private Set<String> parseAllowedDomains(String allowedDomainsStr) {
        if (allowedDomainsStr == null || allowedDomainsStr.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(allowedDomainsStr.split(",")));
    }
}
