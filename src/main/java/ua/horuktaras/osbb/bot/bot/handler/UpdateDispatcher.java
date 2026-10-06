package ua.horuktaras.osbb.bot.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ua.horuktaras.osbb.bot.bot.callback.CallbackQueryHandler;

@Component
public class UpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);

    private final NewMemberHandler newMemberHandler;
    private final MessageHandler messageHandler;
    private final CallbackQueryHandler callbackQueryHandler;

    public UpdateDispatcher(
            NewMemberHandler newMemberHandler,
            MessageHandler messageHandler,
            CallbackQueryHandler callbackQueryHandler
    ) {
        this.newMemberHandler = newMemberHandler;
        this.messageHandler = messageHandler;
        this.callbackQueryHandler = callbackQueryHandler;
    }

    public void dispatch(Update update) {
        if (update.hasMessage()) {
            var message = update.getMessage();
            if (message.getNewChatMembers() != null && !message.getNewChatMembers().isEmpty()) {
                newMemberHandler.handle(update);
            } else {
                messageHandler.handle(update);
            }
        } else if (update.hasCallbackQuery()) {
            callbackQueryHandler.handle(update);
        } else {
            log.debug("Unhandled update type: updateId={}", update.getUpdateId());
        }
    }
}
