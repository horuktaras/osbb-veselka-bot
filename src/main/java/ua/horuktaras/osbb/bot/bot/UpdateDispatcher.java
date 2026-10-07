package ua.horuktaras.osbb.bot.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class UpdateDispatcher {

    private final MessageHandler messageHandler;
    private final CallbackHandler callbackHandler;

    public UpdateDispatcher(MessageHandler messageHandler, CallbackHandler callbackHandler) {
        this.messageHandler = messageHandler;
        this.callbackHandler = callbackHandler;
    }

    public void dispatch(Update update) {
        if (update.hasCallbackQuery()) {
            callbackHandler.handle(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().getFrom() != null) {
            messageHandler.handle(update.getMessage());
        }
    }
}
