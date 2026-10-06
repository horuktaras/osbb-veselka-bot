package ua.horuktaras.osbb.bot.bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.horuktaras.osbb.bot.model.entity.ChatMember;
import ua.horuktaras.osbb.bot.service.ChatMemberService;
import ua.horuktaras.osbb.bot.service.TelegramApiService;
import ua.horuktaras.osbb.bot.util.MessageFormatter;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class UserCommand implements BotCommand {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final ChatMemberService chatMemberService;
    private final TelegramApiService telegramApiService;

    public UserCommand(ChatMemberService chatMemberService, TelegramApiService telegramApiService) {
        this.chatMemberService = chatMemberService;
        this.telegramApiService = telegramApiService;
    }

    @Override
    public String getCommand() { return "user"; }

    @Override
    public String getDescription() { return "Show user info: /user <userId>"; }

    @Override
    public boolean requiresAdmin() { return true; }

    @Override
    public void handle(Update update) {
        var message = update.getMessage();
        Long chatId = message.getChatId();
        String[] parts = message.getText().trim().split("\\s+");

        if (parts.length < 2) {
            sendReply(chatId, "❌ Usage: /user &lt;userId&gt;");
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
        if (memberOpt.isEmpty()) {
            sendReply(chatId, "ℹ️ User " + MessageFormatter.code(String.valueOf(targetId)) + " not found in this chat.");
            return;
        }

        ChatMember member = memberOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("<b>👤 User Info:</b>\n");
        sb.append("ID: ").append(MessageFormatter.code(String.valueOf(targetId))).append("\n");
        sb.append("Verification: <b>").append(member.getVerificationStatus()).append("</b>\n");
        if (member.getVerifiedAt() != null) {
            sb.append("Verified at: ").append(DTF.format(member.getVerifiedAt())).append("\n");
        }
        sb.append("Warnings: <b>").append(member.getWarningCount()).append("</b>\n");
        sb.append("Banned: ").append(member.isBanned() ? "<b>Yes</b>" : "No").append("\n");
        if (member.getMutedUntil() != null) {
            sb.append("Muted until: ").append(DTF.format(member.getMutedUntil())).append("\n");
        } else {
            sb.append("Muted: No\n");
        }
        sb.append("Joined: ").append(DTF.format(member.getJoinedAt())).append("\n");

        sendReply(chatId, sb.toString());
    }

    private void sendReply(Long chatId, String text) {
        telegramApiService.sendMessage(SendMessage.builder()
                .chatId(chatId).text(text).parseMode("HTML").build());
    }
}
