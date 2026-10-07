package ua.horuktaras.osbb.bot.bot;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.config.BotProperties;

import java.util.List;

@Component
public class OsbbBot implements SpringLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(OsbbBot.class);

    private final BotProperties botProperties;
    private final UpdateDispatcher updateDispatcher;
    private final TelegramClient telegramClient;

    public OsbbBot(BotProperties botProperties, UpdateDispatcher updateDispatcher, TelegramClient telegramClient) {
        this.botProperties = botProperties;
        this.updateDispatcher = updateDispatcher;
        this.telegramClient = telegramClient;
    }

    @PostConstruct
    public void registerCommands() {
        try {
            telegramClient.execute(SetMyCommands.builder()
                    .commands(List.of(
                            BotCommand.builder()
                                    .command("osbb")
                                    .description("Подати нову заявку")
                                    .build(),
                            BotCommand.builder()
                                    .command("board")
                                    .description("Панель заявок")
                                    .build()
                    ))
                    .scope(BotCommandScopeAllPrivateChats.builder().build())
                    .build());
            log.info("Bot commands registered");
        } catch (TelegramApiException e) {
            log.error("Failed to register bot commands", e);
        }
    }

    @Override
    public String getBotToken() {
        return botProperties.token();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updates -> updates.forEach(updateDispatcher::dispatch);
    }
}
