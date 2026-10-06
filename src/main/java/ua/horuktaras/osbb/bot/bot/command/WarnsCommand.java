package ua.horuktaras.osbb.bot.bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.horuktaras.osbb.bot.model.entity.ChatMember;
import ua.horuktaras.osbb.bot.model.entity.ModerationLog;
import ua.horuktaras.osbb.bot.model.enums.ModerationAction;
import ua.horuktaras.osbb.bot.service.ChatMemberService;
import ua.horuktaras.osbb.bot.service.ModerationLogService;
import ua.horuktaras.osbb.bot.service.TelegramApiService;
import ua.horuktaras.osbb.bot.util.MessageFormatter;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
public class WarnsCommand implements BotCommand {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final ChatMemberService chatMemberService;
    private final ModerationLogService moderationLogService;
    private final TelegramApiService telegramApiService;

    public WarnsCommand(
            ChatMemberService chatMemberService,
            ModerationLogService moderationLogService,
            TelegramApiService telegramApiService
    ) {
        this.chatMemberService = chatMemberService;
        this.moderationLogService = moderationLogService;
        this.telegramApiService = telegramApiService;
    }

    @Override
    public String getCommand() { return "warns"; }

    @Override
    public String getDescription() { return "Show warning count for a user: /warns <userId>"; }

    @Override
    public boolean requiresAdmin() { return true; }

    @Override
    public void handle(Update update) {
        var message = update.getMessage();
        Long chatId = message.getChatId();
        String[] parts = message.getText().trim().split("\\s+");

        if (parts.length < 2) {
            sendReply(chatId, "❌ Usage: /warns &lt;userId&gt;");
            return;
        }

        Long targetId;
        try {
            targetId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sendReply(chatId, "❌ Invalid user ID: " + parts[1]);
            return;
        }

        Optional<ChatMember> memberOpt = chatMemberService.findMember(chatId, targetId);
        int warningCount = memberOpt.map(ChatMember::getWarningCount).orElse(0);

        List<ModerationLog> warnLogs = moderationLogService.getUserActions(chatId, targetId, 5)
                .stream()
                .filter(l -> l.getAction() == ModerationAction.WARN)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("<b>Warnings for user </b>").append(MessageFormatter.code(String.valueOf(targetId))).append(":\n");
        sb.append("Total warnings: <b>").append(warningCount).append("</b>\n\n");

        if (!warnLogs.isEmpty()) {
            sb.append("<b>Recent warnings:</b>\n");
            for (ModerationLog log : warnLogs) {
                sb.append("• ").append(DTF.format(log.getCreatedAt()));
                if (log.getReason() != null) {
                    sb.append(": ").append(MessageFormatter.escapeHtml(log.getReason()));
                }
                sb.append("\n");
            }
        } else {
            sb.append("No recent warning records.");
        }

        sendReply(chatId, sb.toString());
    }

    private void sendReply(Long chatId, String text) {
        telegramApiService.sendMessage(SendMessage.builder()
                .chatId(chatId).text(text).parseMode("HTML").build());
    }
}
