package ua.horuktaras.osbb.bot.bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ua.horuktaras.osbb.bot.model.entity.ChatConfig;
import ua.horuktaras.osbb.bot.service.ChatConfigService;
import ua.horuktaras.osbb.bot.service.TelegramApiService;

import java.util.List;

@Component
public class SettingsCommand implements BotCommand {

    private final ChatConfigService chatConfigService;
    private final TelegramApiService telegramApiService;

    public SettingsCommand(ChatConfigService chatConfigService, TelegramApiService telegramApiService) {
        this.chatConfigService = chatConfigService;
        this.telegramApiService = telegramApiService;
    }

    @Override
    public String getCommand() { return "settings"; }

    @Override
    public String getDescription() { return "Show and manage chat settings (admin only)"; }

    @Override
    public boolean requiresAdmin() { return true; }

    @Override
    public void handle(Update update) {
        var message = update.getMessage();
        Long chatId = message.getChatId();
        ChatConfig config = chatConfigService.getOrCreate(chatId);

        String text = "<b>⚙️ Chat Settings:</b>\n\n"
                + "• Verification: " + (config.isVerificationEnabled() ? "✅ Enabled" : "❌ Disabled") + "\n"
                + "• Verification Timeout: " + config.getVerificationTimeoutSeconds() + "s ("
                + (config.getVerificationTimeoutSeconds() / 60) + " min)\n"
                + "• Anti-Link: " + (config.isAntiLinkEnabled() ? "✅ Enabled" : "❌ Disabled") + "\n"
                + "• Anti-Spam: " + (config.isAntiSpamEnabled() ? "✅ Enabled" : "❌ Disabled") + "\n"
                + "• Max Messages: " + config.getMaxMessagesPerWindow() + " per "
                + config.getRateLimitWindowSeconds() + "s window\n"
                + "• Warning Limit: " + config.getWarningLimit() + "\n"
                + "• Warning Punishment: " + config.getWarningPunishment() + "\n"
                + "• Punishment Duration: " + config.getWarningPunishmentDurationSeconds() + "s\n";

        if (config.getAllowedDomains() != null && !config.getAllowedDomains().isBlank()) {
            text += "• Allowed Domains: " + config.getAllowedDomains() + "\n";
        }
        if (config.getBlacklistedKeywords() != null && !config.getBlacklistedKeywords().isBlank()) {
            text += "• Blacklisted Keywords: " + config.getBlacklistedKeywords() + "\n";
        }

        InlineKeyboardButton logsBtn = InlineKeyboardButton.builder()
                .text("📋 Recent Logs")
                .callbackData("admin:logs:" + chatId)
                .build();
        InlineKeyboardButton verificationsBtn = InlineKeyboardButton.builder()
                .text("🔍 Verifications")
                .callbackData("admin:verifications:" + chatId)
                .build();
        InlineKeyboardButton statsBtn = InlineKeyboardButton.builder()
                .text("📊 Stats")
                .callbackData("admin:stats:" + chatId)
                .build();

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(logsBtn, verificationsBtn),
                        new InlineKeyboardRow(statsBtn)
                ))
                .build();

        telegramApiService.sendMessage(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(keyboard)
                .build());
    }
}
