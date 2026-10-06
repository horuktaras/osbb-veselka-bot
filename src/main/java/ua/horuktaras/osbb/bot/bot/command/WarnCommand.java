package ua.horuktaras.osbb.bot.bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.dto.ModerationResult;
import ua.horuktaras.osbb.bot.service.ModerationService;
import ua.horuktaras.osbb.bot.service.TelegramApiService;

@Component
public class WarnCommand implements BotCommand {

    private final ModerationService moderationService;
    private final TelegramApiService telegramApiService;
    private final TelegramClient telegramClient;

    public WarnCommand(
            ModerationService moderationService,
            TelegramApiService telegramApiService,
            TelegramClient telegramClient
    ) {
        this.moderationService = moderationService;
        this.telegramApiService = telegramApiService;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getCommand() { return "warn"; }

    @Override
    public String getDescription() { return "Warn a user: /warn <userId> [reason]"; }

    @Override
    public boolean requiresAdmin() { return true; }

    @Override
    public void handle(Update update) {
        var message = update.getMessage();
        Long chatId = message.getChatId();
        Long adminId = message.getFrom().getId();
        String[] parts = message.getText().trim().split("\\s+", 3);

        if (parts.length < 2) {
            sendReply(chatId, "❌ Usage: /warn &lt;userId&gt; [reason]");
            return;
        }

        Long targetId;
        try {
            targetId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sendReply(chatId, "❌ Invalid user ID: " + parts[1]);
            return;
        }

        String reason = parts.length >= 3 ? parts[2] : null;

        ModerationResult result = moderationService.warnUser(chatId, adminId, targetId, reason, telegramClient);
        sendReply(chatId, result.success() ? result.message() : "❌ " + result.message());
    }

    private void sendReply(Long chatId, String text) {
        telegramApiService.sendMessage(SendMessage.builder()
                .chatId(chatId).text(text).parseMode("HTML").build());
    }
}
