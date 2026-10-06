package ua.horuktaras.osbb.bot.bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.dto.ModerationResult;
import ua.horuktaras.osbb.bot.service.ModerationService;
import ua.horuktaras.osbb.bot.service.TelegramApiService;

@Component
public class BanCommand implements BotCommand {

    private final ModerationService moderationService;
    private final TelegramApiService telegramApiService;
    private final TelegramClient telegramClient;

    public BanCommand(
            ModerationService moderationService,
            TelegramApiService telegramApiService,
            TelegramClient telegramClient
    ) {
        this.moderationService = moderationService;
        this.telegramApiService = telegramApiService;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getCommand() { return "ban"; }

    @Override
    public String getDescription() { return "Ban a user: /ban <userId> [reason]"; }

    @Override
    public boolean requiresAdmin() { return true; }

    @Override
    public void handle(Update update) {
        var message = update.getMessage();
        Long chatId = message.getChatId();
        Long adminId = message.getFrom().getId();
        String[] parts = message.getText().trim().split("\\s+", 3);

        if (parts.length < 2) {
            telegramApiService.sendMessage(SendMessage.builder()
                    .chatId(chatId).text("❌ Usage: /ban &lt;userId&gt; [reason]").parseMode("HTML").build());
            return;
        }

        Long targetId;
        try {
            targetId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            telegramApiService.sendMessage(SendMessage.builder()
                    .chatId(chatId).text("❌ Invalid user ID: " + parts[1]).parseMode("HTML").build());
            return;
        }

        String reason = parts.length >= 3 ? parts[2] : null;

        ModerationResult result = moderationService.banUser(chatId, adminId, targetId, reason, telegramClient);
        telegramApiService.sendMessage(SendMessage.builder()
                .chatId(chatId)
                .text(result.success() ? "🔨 " + result.message() : "❌ " + result.message())
                .parseMode("HTML").build());
    }
}
