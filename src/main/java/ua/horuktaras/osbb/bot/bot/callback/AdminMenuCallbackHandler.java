package ua.horuktaras.osbb.bot.bot.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.entity.ChatConfig;
import ua.horuktaras.osbb.bot.model.entity.ModerationLog;
import ua.horuktaras.osbb.bot.model.entity.Verification;
import ua.horuktaras.osbb.bot.service.*;
import ua.horuktaras.osbb.bot.util.MessageFormatter;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class AdminMenuCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminMenuCallbackHandler.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final AdminPermissionService adminPermissionService;
    private final VerificationService verificationService;
    private final ModerationLogService moderationLogService;
    private final ChatConfigService chatConfigService;
    private final ChatMemberService chatMemberService;
    private final TelegramApiService telegramApiService;
    private final TelegramClient telegramClient;

    public AdminMenuCallbackHandler(
            AdminPermissionService adminPermissionService,
            VerificationService verificationService,
            ModerationLogService moderationLogService,
            ChatConfigService chatConfigService,
            ChatMemberService chatMemberService,
            TelegramApiService telegramApiService,
            TelegramClient telegramClient
    ) {
        this.adminPermissionService = adminPermissionService;
        this.verificationService = verificationService;
        this.moderationLogService = moderationLogService;
        this.chatConfigService = chatConfigService;
        this.chatMemberService = chatMemberService;
        this.telegramApiService = telegramApiService;
        this.telegramClient = telegramClient;
    }

    public void handle(Update update) {
        CallbackQuery cbq = update.getCallbackQuery();
        String data = cbq.getData();
        Long requestingUserId = cbq.getFrom().getId();

        // Parse: admin:<action>:<chatId>
        String[] parts = data.split(":");
        if (parts.length < 3) {
            answerCallback(cbq.getId(), "Invalid admin command.", false);
            return;
        }

        long chatId;
        try {
            chatId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            answerCallback(cbq.getId(), "Invalid chat ID.", false);
            return;
        }

        if (!adminPermissionService.isAdmin(chatId, requestingUserId, telegramClient)) {
            answerCallback(cbq.getId(), "You don't have admin permissions.", true);
            return;
        }

        String action = parts[1];
        switch (action) {
            case "verifications" -> handleVerifications(cbq, chatId);
            case "logs" -> handleLogs(cbq, chatId);
            case "config" -> handleConfig(cbq, chatId);
            case "stats" -> handleStats(cbq, chatId);
            default -> answerCallback(cbq.getId(), "Unknown admin action.", false);
        }
    }

    private void handleVerifications(CallbackQuery cbq, long chatId) {
        List<Verification> pending = verificationService.findExpiredPendingVerifications();
        StringBuilder sb = new StringBuilder();
        sb.append("<b>Pending Verifications in this chat:</b>\n");
        long count = pending.stream().filter(v -> v.getChatId() == chatId).count();
        if (count == 0) {
            sb.append("No pending verifications.");
        } else {
            pending.stream()
                    .filter(v -> v.getChatId() == chatId)
                    .forEach(v -> sb.append("• User ID: ").append(MessageFormatter.code(String.valueOf(v.getTelegramUserId())))
                            .append(" | Expires: ").append(DTF.format(v.getExpiresAt())).append("\n"));
        }
        answerCallback(cbq.getId(), null, false);
        sendReply(chatId, sb.toString());
    }

    private void handleLogs(CallbackQuery cbq, long chatId) {
        List<ModerationLog> logs = moderationLogService.getRecentActions(chatId, 10);
        StringBuilder sb = new StringBuilder();
        sb.append("<b>Recent Moderation Actions:</b>\n");
        if (logs.isEmpty()) {
            sb.append("No moderation actions found.");
        } else {
            for (ModerationLog entry : logs) {
                sb.append("• <b>").append(entry.getAction()).append("</b> → User ")
                        .append(MessageFormatter.code(String.valueOf(entry.getTargetTelegramUserId())))
                        .append(" at ").append(DTF.format(entry.getCreatedAt()));
                if (entry.getReason() != null) {
                    sb.append(" | Reason: ").append(MessageFormatter.escapeHtml(entry.getReason()));
                }
                sb.append("\n");
            }
        }
        answerCallback(cbq.getId(), null, false);
        sendReply(chatId, sb.toString());
    }

    private void handleConfig(CallbackQuery cbq, long chatId) {
        ChatConfig config = chatConfigService.getOrCreate(chatId);
        String text = "<b>Current Chat Configuration:</b>\n"
                + "• Verification: " + (config.isVerificationEnabled() ? "✅ Enabled" : "❌ Disabled") + "\n"
                + "• Verification Timeout: " + config.getVerificationTimeoutSeconds() + "s\n"
                + "• Anti-Link: " + (config.isAntiLinkEnabled() ? "✅ Enabled" : "❌ Disabled") + "\n"
                + "• Anti-Spam: " + (config.isAntiSpamEnabled() ? "✅ Enabled" : "❌ Disabled") + "\n"
                + "• Max Messages/Window: " + config.getMaxMessagesPerWindow() + "\n"
                + "• Rate Limit Window: " + config.getRateLimitWindowSeconds() + "s\n"
                + "• Warning Limit: " + config.getWarningLimit() + "\n"
                + "• Warning Punishment: " + config.getWarningPunishment() + "\n"
                + "• Punishment Duration: " + config.getWarningPunishmentDurationSeconds() + "s\n";
        answerCallback(cbq.getId(), null, false);
        sendReply(chatId, text);
    }

    private void handleStats(CallbackQuery cbq, long chatId) {
        Long usersWithWarnings = chatMemberService.findMember(chatId, 0L)
                .map(m -> 0L).orElse(0L);
        // Use repository count
        String text = "<b>Warning Statistics:</b>\n"
                + "Stats are available via the /warns command per user.\n"
                + "Use /user &lt;userId&gt; to check individual user status.";
        answerCallback(cbq.getId(), null, false);
        sendReply(chatId, text);
    }

    private void answerCallback(String callbackId, String text, boolean showAlert) {
        try {
            AnswerCallbackQuery.AnswerCallbackQueryBuilder builder = AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .showAlert(showAlert);
            if (text != null) {
                builder.text(text);
            }
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.warn("Failed to answer admin callback: {}", e.getMessage());
        }
    }

    private void sendReply(long chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .build();
        telegramApiService.sendMessage(msg);
    }
}
