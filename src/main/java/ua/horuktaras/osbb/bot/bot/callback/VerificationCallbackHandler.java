package ua.horuktaras.osbb.bot.bot.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.enums.ModerationAction;
import ua.horuktaras.osbb.bot.service.ChatMemberService;
import ua.horuktaras.osbb.bot.service.ModerationLogService;
import ua.horuktaras.osbb.bot.service.TelegramApiService;
import ua.horuktaras.osbb.bot.service.VerificationService;
import ua.horuktaras.osbb.bot.util.MessageFormatter;

@Component
public class VerificationCallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(VerificationCallbackHandler.class);
    private static final String PREFIX = "verify:agree:";

    private final VerificationService verificationService;
    private final ChatMemberService chatMemberService;
    private final TelegramApiService telegramApiService;
    private final ModerationLogService moderationLogService;
    private final TelegramClient telegramClient;

    public VerificationCallbackHandler(
            VerificationService verificationService,
            ChatMemberService chatMemberService,
            TelegramApiService telegramApiService,
            ModerationLogService moderationLogService,
            TelegramClient telegramClient
    ) {
        this.verificationService = verificationService;
        this.chatMemberService = chatMemberService;
        this.telegramApiService = telegramApiService;
        this.moderationLogService = moderationLogService;
        this.telegramClient = telegramClient;
    }

    public void handle(Update update) {
        CallbackQuery cbq = update.getCallbackQuery();
        String data = cbq.getData();

        if (!data.startsWith(PREFIX)) {
            answerCallback(cbq.getId(), "Invalid verification data.", false);
            return;
        }

        String[] parts = data.substring(PREFIX.length()).split(":");
        if (parts.length != 2) {
            log.warn("Invalid verification callback data: {}", data);
            answerCallback(cbq.getId(), "Invalid verification data.", false);
            return;
        }

        long chatId;
        long userId;
        try {
            chatId = Long.parseLong(parts[0]);
            userId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse chatId/userId from callback: {}", data);
            answerCallback(cbq.getId(), "Invalid verification data.", false);
            return;
        }

        Long requestingUserId = cbq.getFrom().getId();

        // Validate that the correct user clicked
        if (!requestingUserId.equals(userId)) {
            answerCallback(cbq.getId(), "This verification button is not for you.", true);
            return;
        }

        boolean completed = verificationService.completeVerification(chatId, userId, requestingUserId);

        if (completed) {
            // Unrestrict user
            telegramApiService.unrestrictUser(chatId, userId);
            chatMemberService.markVerified(chatId, userId);

            moderationLogService.log(chatId, null, userId, ModerationAction.VERIFICATION_PASSED,
                    "User passed verification", null);

            answerCallback(cbq.getId(), "✅ Verification successful! Welcome to the community!", false);

            String displayName = cbq.getFrom().getFirstName() != null
                    ? cbq.getFrom().getFirstName() : "User";
            SendMessage welcomeMsg = SendMessage.builder()
                    .chatId(chatId)
                    .text("✅ " + MessageFormatter.formatUserMention(userId, displayName)
                            + " has been verified and can now participate in the chat!")
                    .parseMode("HTML")
                    .build();
            telegramApiService.sendMessage(welcomeMsg);

            log.info("User userId={} verified successfully in chatId={}", userId, chatId);
        } else {
            // No pending verification found - either already verified or expired
            answerCallback(cbq.getId(), null, false);
            log.debug("Verification not found or already processed for userId={} in chatId={}", userId, chatId);
        }
    }

    private void answerCallback(String callbackId, String text, boolean showAlert) {
        try {
            AnswerCallbackQuery.AnswerCallbackQueryBuilder builder = AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .showAlert(showAlert);
            if (text != null) {
                builder.text(text);
            }
            telegramClient.execute(builder.build());
        } catch (TelegramApiException e) {
            log.warn("Failed to answer callback: {}", e.getMessage());
        }
    }
}
