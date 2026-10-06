package ua.horuktaras.osbb.bot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.horuktaras.osbb.bot.bot.handler.UpdateDispatcher;
import ua.horuktaras.osbb.bot.config.BotProperties;
import ua.horuktaras.osbb.bot.service.UpdateIdempotencyService;

@Component
public class OsbbBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(OsbbBot.class);

    private final BotProperties botProperties;
    private final UpdateDispatcher updateDispatcher;
    private final UpdateIdempotencyService idempotencyService;

    public OsbbBot(
            BotProperties botProperties,
            UpdateDispatcher updateDispatcher,
            UpdateIdempotencyService idempotencyService
    ) {
        this.botProperties = botProperties;
        this.updateDispatcher = updateDispatcher;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public String getBotToken() {
        return botProperties.token();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.getUpdateId() != null) {
            if (idempotencyService.isAlreadyProcessed(update.getUpdateId())) {
                log.debug("Skipping already processed update: {}", update.getUpdateId());
                return;
            }
            idempotencyService.markProcessed(update.getUpdateId());
        }

        try {
            updateDispatcher.dispatch(update);
        } catch (Exception e) {
            log.error("Error processing update {}: {}", update.getUpdateId(), e.getMessage(), e);
        }
    }
}
