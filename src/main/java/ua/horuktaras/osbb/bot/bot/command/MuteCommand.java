package ua.horuktaras.osbb.bot.bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.dto.ModerationResult;
import ua.horuktaras.osbb.bot.service.ModerationService;
import ua.horuktaras.osbb.bot.service.TelegramApiService;
import ua.horuktaras.osbb.bot.util.DurationParser;

import java.time.Duration;

@Component
public class MuteCommand implements BotCommand {

    private final ModerationService moderationService;
    private final TelegramApiService telegramApiService;
    private final TelegramClient telegramClient;

    public MuteCommand(
            ModerationService moderationService,
            TelegramApiService telegramApiService,
            TelegramClient telegramClient
    ) {
        this.moderationService = moderationService;
        this.telegramApiService = telegramApiService;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getCommand() { return "mute"; }

    @Override
    public String getDescription() { return "Mute a user: /mute <userId> <duration> [reason] (e.g. 30m, 2h, 1d)"; }

    @Override
    public boolean requiresAdmin() { return true; }

    @Override
    public void handle(Update update) {
        var message = update.getMessage();
        Long chatId = message.getChatId();
        Long adminId = message.getFrom().getId();
        String[] parts = message.getText().trim().split("\\s+", 4);

        if (parts.length < 3) {
            sendReply(chatId, "❌ Usage: /mute &lt;userId&gt; &lt;duration&gt; [reason]\nExamples: 30m, 2h, 1d, 7d");
            return;
        }

        Long targetId;
        try {
            targetId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sendReply(chatId, "❌ Invalid user ID: " + parts[1]);
            return;
        }

        Duration duration;
        try {
            duration = DurationParser.parse(parts[2]);
        } catch (IllegalArgumentException e) {
            sendReply(chatId, "❌ Invalid duration: " + parts[2] + "\nExamples: 30m, 2h, 1d, 7d");
            return;
        }

        String reason = parts.length >= 4 ? parts[3] : null;

        ModerationResult result = moderationService.muteUser(chatId, adminId, targetId, duration, reason, telegramClient);
        sendReply(chatId, result.success() ? result.message() : "❌ " + result.message());
    }

    private void sendReply(Long chatId, String text) {
        telegramApiService.sendMessage(SendMessage.builder()
                .chatId(chatId).text(text).parseMode("HTML").build());
    }
}
