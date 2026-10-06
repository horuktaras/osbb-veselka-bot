package ua.horuktaras.osbb.bot.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.horuktaras.osbb.bot.model.entity.Verification;
import ua.horuktaras.osbb.bot.model.enums.ModerationAction;
import ua.horuktaras.osbb.bot.service.ChatMemberService;
import ua.horuktaras.osbb.bot.service.ModerationLogService;
import ua.horuktaras.osbb.bot.service.TelegramApiService;
import ua.horuktaras.osbb.bot.service.VerificationService;

import java.util.List;

@Component
public class VerificationExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(VerificationExpirationScheduler.class);

    private final VerificationService verificationService;
    private final TelegramApiService telegramApiService;
    private final ChatMemberService chatMemberService;
    private final ModerationLogService moderationLogService;

    public VerificationExpirationScheduler(
            VerificationService verificationService,
            TelegramApiService telegramApiService,
            ChatMemberService chatMemberService,
            ModerationLogService moderationLogService
    ) {
        this.verificationService = verificationService;
        this.telegramApiService = telegramApiService;
        this.chatMemberService = chatMemberService;
        this.moderationLogService = moderationLogService;
    }

    @Scheduled(fixedDelay = 60000)
    public void expireTimedOutVerifications() {
        List<Verification> expired = verificationService.findExpiredPendingVerifications();
        if (expired.isEmpty()) return;

        log.info("Processing {} expired verifications", expired.size());
        for (Verification v : expired) {
            try {
                Long chatId = v.getChatId();
                Long userId = v.getTelegramUserId();

                // Kick the user (ban then immediately unban to remove them from chat)
                boolean kicked = telegramApiService.banUser(chatId, userId, null);
                if (kicked) {
                    telegramApiService.unbanUser(chatId, userId);
                    chatMemberService.ban(chatId, userId);
                    chatMemberService.unban(chatId, userId);
                }

                verificationService.expireVerification(v);

                moderationLogService.log(chatId, null, userId, ModerationAction.VERIFICATION_EXPIRED,
                        "Verification timeout expired", null);

                log.info("Kicked userId={} from chatId={} due to verification timeout", userId, chatId);
            } catch (Exception e) {
                log.error("Error processing expired verification id={}: {}", v.getId(), e.getMessage(), e);
            }
        }
    }
}
