package ua.horuktaras.osbb.bot.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ua.horuktaras.osbb.bot.model.entity.ChatConfig;
import ua.horuktaras.osbb.bot.model.entity.Verification;
import ua.horuktaras.osbb.bot.model.enums.ModerationAction;
import ua.horuktaras.osbb.bot.service.*;
import ua.horuktaras.osbb.bot.util.MessageFormatter;
import ua.horuktaras.osbb.bot.util.UserUtils;
import ua.horuktaras.osbb.bot.verification.VerificationKeyboardFactory;

import java.util.Optional;

@Component
public class NewMemberHandler {

    private static final Logger log = LoggerFactory.getLogger(NewMemberHandler.class);

    private final TelegramUserService telegramUserService;
    private final ChatMemberService chatMemberService;
    private final ChatConfigService chatConfigService;
    private final VerificationService verificationService;
    private final TelegramApiService telegramApiService;
    private final ModerationLogService moderationLogService;
    private final VerificationKeyboardFactory keyboardFactory;

    public NewMemberHandler(
            TelegramUserService telegramUserService,
            ChatMemberService chatMemberService,
            ChatConfigService chatConfigService,
            VerificationService verificationService,
            TelegramApiService telegramApiService,
            ModerationLogService moderationLogService,
            VerificationKeyboardFactory keyboardFactory
    ) {
        this.telegramUserService = telegramUserService;
        this.chatMemberService = chatMemberService;
        this.chatConfigService = chatConfigService;
        this.verificationService = verificationService;
        this.telegramApiService = telegramApiService;
        this.moderationLogService = moderationLogService;
        this.keyboardFactory = keyboardFactory;
    }

    public void handle(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();

        for (User newMember : message.getNewChatMembers()) {
            if (newMember.getIsBot()) {
                log.debug("Skipping bot member in chatId={}", chatId);
                continue;
            }

            Long userId = newMember.getId();
            log.info("New member userId={} in chatId={}", userId, chatId);

            telegramUserService.getOrCreate(newMember);
            chatMemberService.getOrCreate(chatId, userId);

            ChatConfig config = chatConfigService.getOrCreate(chatId);

            if (config.isVerificationEnabled()) {
                handleVerification(chatId, userId, newMember, config);
            } else {
                chatMemberService.markVerified(chatId, userId);
                log.debug("Verification disabled, marked userId={} as verified in chatId={}", userId, chatId);
            }
        }
    }

    private void handleVerification(Long chatId, Long userId, User newMember, ChatConfig config) {
        // Restrict user from sending messages
        ChatPermissions restrictedPermissions = ChatPermissions.builder()
                .canSendMessages(false)
                .canSendAudios(false)
                .canSendDocuments(false)
                .canSendPhotos(false)
                .canSendVideos(false)
                .canSendVideoNotes(false)
                .canSendVoiceNotes(false)
                .canSendOtherMessages(false)
                .canAddWebPagePreviews(false)
                .build();
        telegramApiService.restrictUser(chatId, userId, restrictedPermissions);

        // Create verification record
        Verification verification = verificationService.startVerification(
                chatId, userId, config.getVerificationTimeoutSeconds());

        // Send verification message
        String displayName = UserUtils.getDisplayName(newMember);
        String verificationText = "👋 Welcome, " + MessageFormatter.formatUserMention(userId, displayName) + "!\n\n"
                + "Please confirm that you agree to the community rules by clicking the button below.\n"
                + "You have " + (config.getVerificationTimeoutSeconds() / 60) + " minutes to complete verification.";

        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(verificationText)
                .parseMode("HTML")
                .replyMarkup(keyboardFactory.createVerificationKeyboard(chatId, userId))
                .build();

        Optional<org.telegram.telegrambots.meta.api.objects.message.Message> sent =
                telegramApiService.sendMessage(sendMessage);

        sent.ifPresent(m -> verificationService.setVerificationMessageId(
                verification.getId(), m.getMessageId().longValue()));

        moderationLogService.log(chatId, null, userId, ModerationAction.VERIFICATION_STARTED,
                "New member verification started", null);

        log.info("Verification started for userId={} in chatId={}", userId, chatId);
    }
}
