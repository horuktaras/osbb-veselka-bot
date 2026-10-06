package ua.horuktaras.osbb.bot.bot.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ua.horuktaras.osbb.bot.service.AdminPermissionService;
import ua.horuktaras.osbb.bot.service.TelegramApiService;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommandProcessor {

    private static final Logger log = LoggerFactory.getLogger(CommandProcessor.class);

    private final Map<String, BotCommand> commandMap;
    private final AdminPermissionService adminPermissionService;
    private final TelegramApiService telegramApiService;
    private final TelegramClient telegramClient;

    public CommandProcessor(
            List<BotCommand> commands,
            AdminPermissionService adminPermissionService,
            TelegramApiService telegramApiService,
            TelegramClient telegramClient
    ) {
        this.commandMap = new HashMap<>();
        for (BotCommand cmd : commands) {
            commandMap.put(cmd.getCommand().toLowerCase(), cmd);
        }
        this.adminPermissionService = adminPermissionService;
        this.telegramApiService = telegramApiService;
        this.telegramClient = telegramClient;
        log.info("Registered commands: {}", commandMap.keySet());
    }

    public void process(Update update) {
        Message message = update.getMessage();
        if (message == null || message.getText() == null) return;

        String text = message.getText().trim();
        if (!text.startsWith("/")) return;

        // Extract command: /command@botname args → command
        String commandPart = text.split("\\s+")[0].substring(1);
        // Remove bot mention if present
        if (commandPart.contains("@")) {
            commandPart = commandPart.substring(0, commandPart.indexOf('@'));
        }
        commandPart = commandPart.toLowerCase();

        BotCommand command = commandMap.get(commandPart);
        if (command == null) {
            log.debug("Unknown command: {}", commandPart);
            return;
        }

        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        if (command.requiresAdmin()) {
            if (!adminPermissionService.isAdmin(chatId, userId, telegramClient)) {
                SendMessage permDenied = SendMessage.builder()
                        .chatId(chatId)
                        .text("⛔ You don't have permission to use this command.")
                        .parseMode("HTML")
                        .build();
                telegramApiService.sendMessage(permDenied);
                return;
            }
        }

        try {
            command.handle(update);
        } catch (Exception e) {
            log.error("Error executing command /{}: {}", commandPart, e.getMessage(), e);
            SendMessage errorMsg = SendMessage.builder()
                    .chatId(chatId)
                    .text("❌ An error occurred while processing the command.")
                    .parseMode("HTML")
                    .build();
            telegramApiService.sendMessage(errorMsg);
        }
    }
}
