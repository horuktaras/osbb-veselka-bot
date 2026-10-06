package ua.horuktaras.osbb.bot.bot.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class CallbackQueryHandler {

    private static final Logger log = LoggerFactory.getLogger(CallbackQueryHandler.class);

    private final VerificationCallbackHandler verificationCallbackHandler;
    private final AdminMenuCallbackHandler adminMenuCallbackHandler;
    private final TelegramClient telegramClient;

    public CallbackQueryHandler(
            VerificationCallbackHandler verificationCallbackHandler,
            AdminMenuCallbackHandler adminMenuCallbackHandler,
            TelegramClient telegramClient
    ) {
        this.verificationCallbackHandler = verificationCallbackHandler;
        this.adminMenuCallbackHandler = adminMenuCallbackHandler;
        this.telegramClient = telegramClient;
    }

    public void handle(Update update) {
        var cbq = update.getCallbackQuery();
        if (cbq == null) return;

        String data = cbq.getData();
        if (data == null) {
            answerCallback(cbq.getId(), null, false);
            return;
        }

        try {
            if (data.startsWith("verify:")) {
                verificationCallbackHandler.handle(update);
            } else if (data.startsWith("admin:")) {
                adminMenuCallbackHandler.handle(update);
            } else {
                log.warn("Unknown callback data: {}", data);
                answerCallback(cbq.getId(), "Unknown action.", false);
            }
        } catch (Exception e) {
            log.error("Error handling callback query: {}", e.getMessage(), e);
            answerCallback(cbq.getId(), "An error occurred.", false);
        }
    }

    private void answerCallback(String callbackId, String text, boolean showAlert) {
        try {
            AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .text(text)
                    .showAlert(showAlert)
                    .build();
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            log.warn("Failed to answer callback query: {}", e.getMessage());
        }
    }
}
