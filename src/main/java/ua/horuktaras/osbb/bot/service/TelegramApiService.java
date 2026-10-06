package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TelegramApiService {

    private static final Logger log = LoggerFactory.getLogger(TelegramApiService.class);
    private static final int MAX_RETRIES = 3;

    private final TelegramClient telegramClient;

    public TelegramApiService(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    public boolean restrictUser(Long chatId, Long userId, ChatPermissions permissions) {
        RestrictChatMember restrict = RestrictChatMember.builder()
                .chatId(chatId)
                .userId(userId)
                .permissions(permissions)
                .build();
        return executeWithRetry(() -> {
            telegramClient.execute(restrict);
            return true;
        }, "restrictUser", chatId, userId);
    }

    public boolean unrestrictUser(Long chatId, Long userId) {
        ChatPermissions fullPermissions = ChatPermissions.builder()
                .canSendMessages(true)
                .canSendAudios(true)
                .canSendDocuments(true)
                .canSendPhotos(true)
                .canSendVideos(true)
                .canSendVideoNotes(true)
                .canSendVoiceNotes(true)
                .canSendOtherMessages(true)
                .canAddWebPagePreviews(true)
                .canSendPolls(true)
                .canChangeInfo(false)
                .canInviteUsers(true)
                .canPinMessages(false)
                .build();
        RestrictChatMember restrict = RestrictChatMember.builder()
                .chatId(chatId)
                .userId(userId)
                .permissions(fullPermissions)
                .build();
        return executeWithRetry(() -> {
            telegramClient.execute(restrict);
            return true;
        }, "unrestrictUser", chatId, userId);
    }

    public boolean banUser(Long chatId, Long userId, Duration duration) {
        BanChatMember.BanChatMemberBuilder builder = BanChatMember.builder()
                .chatId(chatId)
                .userId(userId);
        if (duration != null && !duration.isZero() && !duration.isNegative()) {
            builder.untilDate((int) Instant.now().plus(duration).getEpochSecond());
        }
        BanChatMember ban = builder.build();
        try {
            telegramClient.execute(ban);
            log.info("Banned userId={} from chatId={} duration={}", userId, chatId, duration);
            return true;
        } catch (TelegramApiRequestException e) {
            log.warn("Failed to ban userId={} from chatId={}: code={} msg={}",
                    userId, chatId, e.getErrorCode(), e.getApiResponse());
            return false;
        } catch (TelegramApiException e) {
            log.error("Error banning userId={} from chatId={}", userId, chatId, e);
            return false;
        }
    }

    public boolean unbanUser(Long chatId, Long userId) {
        UnbanChatMember unban = UnbanChatMember.builder()
                .chatId(chatId)
                .userId(userId)
                .onlyIfBanned(true)
                .build();
        try {
            telegramClient.execute(unban);
            log.info("Unbanned userId={} from chatId={}", userId, chatId);
            return true;
        } catch (TelegramApiRequestException e) {
            log.warn("Failed to unban userId={} from chatId={}: code={} msg={}",
                    userId, chatId, e.getErrorCode(), e.getApiResponse());
            return false;
        } catch (TelegramApiException e) {
            log.error("Error unbanning userId={} from chatId={}", userId, chatId, e);
            return false;
        }
    }

    public boolean deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage delete = DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        return executeWithRetry(() -> {
            telegramClient.execute(delete);
            return true;
        }, "deleteMessage", chatId, (long) messageId);
    }

    public Optional<Message> sendMessage(SendMessage message) {
        return executeWithRetryOptional(() -> telegramClient.execute(message),
                "sendMessage", Long.parseLong(message.getChatId()), 0L);
    }

    public List<ChatMember> getChatAdmins(Long chatId) {
        try {
            GetChatAdministrators request = new GetChatAdministrators(String.valueOf(chatId));
            return telegramClient.execute(request);
        } catch (TelegramApiException e) {
            log.warn("Failed to get admins for chatId={}: {}", chatId, e.getMessage());
            return List.of();
        }
    }

    @FunctionalInterface
    private interface TelegramAction<T> {
        T execute() throws TelegramApiException;
    }

    private boolean executeWithRetry(TelegramAction<Boolean> action, String operation, Long chatId, Long targetId) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                return action.execute();
            } catch (TelegramApiRequestException e) {
                int code = e.getErrorCode();
                if (code == 403) {
                    log.warn("{}: bot blocked or no permission chatId={} targetId={}: {}", operation, chatId, targetId, e.getApiResponse());
                    return false;
                }
                if (code == 400) {
                    log.warn("{}: bad request chatId={} targetId={}: {}", operation, chatId, targetId, e.getApiResponse());
                    return false;
                }
                if (code == 429) {
                    attempt++;
                    long retryAfter = e.getParameters() != null && e.getParameters().getRetryAfter() != null
                            ? e.getParameters().getRetryAfter() * 1000L
                            : (long) Math.pow(2, attempt) * 1000L;
                    log.warn("{}: rate limited, waiting {}ms attempt={}", operation, retryAfter, attempt);
                    sleepSafe(retryAfter);
                } else {
                    attempt++;
                    if (attempt >= MAX_RETRIES) {
                        log.error("{}: failed after {} attempts chatId={}: {}", operation, attempt, chatId, e.getMessage());
                        return false;
                    }
                    sleepSafe((long) Math.pow(2, attempt) * 500L);
                }
            } catch (TelegramApiException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("{}: failed after {} attempts chatId={}", operation, attempt, chatId, e);
                    return false;
                }
                sleepSafe((long) Math.pow(2, attempt) * 500L);
            }
        }
        return false;
    }

    private <T> Optional<T> executeWithRetryOptional(TelegramAction<T> action, String operation, Long chatId, Long targetId) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                T result = action.execute();
                return Optional.ofNullable(result);
            } catch (TelegramApiRequestException e) {
                int code = e.getErrorCode();
                if (code == 403 || code == 400) {
                    log.warn("{}: API error chatId={}: code={} {}", operation, chatId, code, e.getApiResponse());
                    return Optional.empty();
                }
                if (code == 429) {
                    attempt++;
                    long retryAfter = e.getParameters() != null && e.getParameters().getRetryAfter() != null
                            ? e.getParameters().getRetryAfter() * 1000L
                            : (long) Math.pow(2, attempt) * 1000L;
                    log.warn("{}: rate limited, waiting {}ms attempt={}", operation, retryAfter, attempt);
                    sleepSafe(retryAfter);
                } else {
                    attempt++;
                    if (attempt >= MAX_RETRIES) {
                        log.error("{}: failed after {} attempts chatId={}: {}", operation, attempt, chatId, e.getMessage());
                        return Optional.empty();
                    }
                    sleepSafe((long) Math.pow(2, attempt) * 500L);
                }
            } catch (TelegramApiException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("{}: failed after {} attempts chatId={}", operation, attempt, chatId, e);
                    return Optional.empty();
                }
                sleepSafe((long) Math.pow(2, attempt) * 500L);
            }
        }
        return Optional.empty();
    }

    private void sleepSafe(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
