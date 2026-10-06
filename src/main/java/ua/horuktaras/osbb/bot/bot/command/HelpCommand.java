package ua.horuktaras.osbb.bot.bot.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.horuktaras.osbb.bot.service.TelegramApiService;

import java.util.List;

@Component
public class HelpCommand implements BotCommand {

    private final TelegramApiService telegramApiService;
    private final List<BotCommand> commands;

    public HelpCommand(TelegramApiService telegramApiService, List<BotCommand> commands) {
        this.telegramApiService = telegramApiService;
        this.commands = commands;
    }

    @Override
    public String getCommand() { return "help"; }

    @Override
    public String getDescription() { return "Show available commands"; }

    @Override
    public boolean requiresAdmin() { return false; }

    @Override
    public void handle(Update update) {
        Long chatId = update.getMessage().getChatId();

        StringBuilder sb = new StringBuilder();
        sb.append("<b>🤖 OSBB Veselka Bot Commands:</b>\n\n");

        for (BotCommand cmd : commands) {
            if (cmd.getCommand().equals("help")) continue;
            sb.append("/").append(cmd.getCommand());
            if (cmd.requiresAdmin()) {
                sb.append(" <i>(admin)</i>");
            }
            sb.append(" — ").append(cmd.getDescription()).append("\n");
        }

        sb.append("\n<i>Commands marked (admin) require administrator privileges.</i>");

        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(sb.toString())
                .parseMode("HTML")
                .build();
        telegramApiService.sendMessage(msg);
    }
}
