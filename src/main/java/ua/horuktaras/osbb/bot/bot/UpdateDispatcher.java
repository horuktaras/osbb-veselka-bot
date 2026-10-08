package ua.horuktaras.osbb.bot.bot;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.TimeUnit;

@Component
public class UpdateDispatcher {

    private final MessageHandler messageHandler;
    private final CallbackHandler callbackHandler;
    private final Cache<Integer, Boolean> processedUpdates;

    public UpdateDispatcher(MessageHandler messageHandler, CallbackHandler callbackHandler) {
        this.messageHandler = messageHandler;
        this.callbackHandler = callbackHandler;
        this.processedUpdates = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(10_000)
                .build();
    }

    public void dispatch(Update update) {
        if (processedUpdates.getIfPresent(update.getUpdateId()) != null) {
            return;
        }
        processedUpdates.put(update.getUpdateId(), Boolean.TRUE);

        if (update.hasCallbackQuery()) {
            callbackHandler.handle(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().getFrom() != null) {
            messageHandler.handle(update.getMessage());
        }
    }
}
