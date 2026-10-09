package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.entity.Request;
import ua.horuktaras.osbb.bot.model.enums.MediaType;
import ua.horuktaras.osbb.bot.model.enums.RequestStatus;
import ua.horuktaras.osbb.bot.service.AdminStatusCommentService.PendingStatusChange;

@Service
public class StatusChangeService {

    private static final Logger log = LoggerFactory.getLogger(StatusChangeService.class);

    private final RequestService requestService;
    private final AdminNotificationService adminNotificationService;
    private final AdminBoardService adminBoardService;
    private final TelegramClient telegramClient;

    public StatusChangeService(RequestService requestService,
                                AdminNotificationService adminNotificationService,
                                AdminBoardService adminBoardService,
                                TelegramClient telegramClient) {
        this.requestService = requestService;
        this.adminNotificationService = adminNotificationService;
        this.adminBoardService = adminBoardService;
        this.telegramClient = telegramClient;
    }

    /**
     * Applies status change with optional comment, then updates the triggering message.
     * If the message was the original admin notification — delegates to AdminNotificationService.
     * Otherwise treats it as a board detail message and re-renders in place.
     */
    public void apply(PendingStatusChange pending, String comment) {
        Request updated = requestService.updateStatus(pending.requestId(), pending.newStatus(), comment);

        boolean isAdminNotification = updated.getAdminChatMessageId() != null
                && updated.getAdminChatMessageId().equals(pending.messageId().longValue());

        if (isAdminNotification) {
            adminNotificationService.updateRequestMessage(updated);
        } else {
            AdminBoardService.BoardMessage boardMsg = adminBoardService.buildDetailMessage(updated, 0, "ALL");
            try {
                if (pending.mediaMessage()) {
                    telegramClient.execute(DeleteMessage.builder()
                            .chatId(pending.chatId()).messageId(pending.messageId()).build());
                    sendMediaDetail(pending.chatId(), updated, boardMsg);
                } else {
                    telegramClient.execute(EditMessageText.builder()
                            .chatId(pending.chatId())
                            .messageId(pending.messageId())
                            .text(boardMsg.text())
                            .parseMode("HTML")
                            .replyMarkup(boardMsg.keyboard())
                            .build());
                }
            } catch (TelegramApiException e) {
                log.warn("Failed to update board message after status change", e);
            }
        }
    }

    public void sendMediaDetail(Long chatId, Request request, AdminBoardService.BoardMessage boardMsg) throws TelegramApiException {
        String caption = boardMsg.text().length() > 1024
                ? boardMsg.text().substring(0, 1021) + "..."
                : boardMsg.text();
        if (request.getMediaType() == MediaType.PHOTO) {
            telegramClient.execute(SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(request.getMediaFileId()))
                    .caption(caption)
                    .parseMode("HTML")
                    .replyMarkup(boardMsg.keyboard())
                    .build());
        } else {
            telegramClient.execute(SendVideo.builder()
                    .chatId(chatId)
                    .video(new InputFile(request.getMediaFileId()))
                    .caption(caption)
                    .parseMode("HTML")
                    .replyMarkup(boardMsg.keyboard())
                    .build());
        }
    }

    public boolean isValidTransition(Request request, RequestStatus newStatus) {
        return request.getStatus().nextStatuses().contains(newStatus);
    }
}
