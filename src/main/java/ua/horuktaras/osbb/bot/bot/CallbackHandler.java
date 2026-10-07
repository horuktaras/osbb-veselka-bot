package ua.horuktaras.osbb.bot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.dto.RequestDraft;
import ua.horuktaras.osbb.bot.model.entity.Request;
import ua.horuktaras.osbb.bot.model.enums.ConversationStep;
import ua.horuktaras.osbb.bot.model.enums.RequestStatus;
import ua.horuktaras.osbb.bot.model.enums.RequestType;
import ua.horuktaras.osbb.bot.service.AdminBoardService;
import ua.horuktaras.osbb.bot.service.AdminNotificationService;
import ua.horuktaras.osbb.bot.service.ConversationService;
import ua.horuktaras.osbb.bot.service.RequestService;

import java.util.Optional;

@Component
public class CallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(CallbackHandler.class);

    private final TelegramClient telegramClient;
    private final ConversationService conversationService;
    private final RequestService requestService;
    private final AdminNotificationService adminNotificationService;
    private final AdminBoardService adminBoardService;
    private final MessageHandler messageHandler;

    public CallbackHandler(TelegramClient telegramClient,
                           ConversationService conversationService,
                           RequestService requestService,
                           AdminNotificationService adminNotificationService,
                           AdminBoardService adminBoardService,
                           MessageHandler messageHandler) {
        this.telegramClient = telegramClient;
        this.conversationService = conversationService;
        this.requestService = requestService;
        this.adminNotificationService = adminNotificationService;
        this.adminBoardService = adminBoardService;
        this.messageHandler = messageHandler;
    }

    public void handle(CallbackQuery callback) {
        String data = callback.getData();
        Long userId = callback.getFrom().getId();
        Long chatId = callback.getMessage().getChatId();

        if (data.startsWith("status:")) {
            handleStatusChange(callback, data, chatId);
        } else if (data.startsWith("urgency:")) {
            handleUrgency(callback, data, userId, chatId);
        } else if (data.startsWith("type:")) {
            handleType(callback, data, userId, chatId);
        } else if (data.equals("media:skip")) {
            handleMediaSkip(callback, userId, chatId);
        } else if (data.startsWith("confirm:")) {
            handleConfirmation(callback, data, userId, chatId);
        } else if (data.startsWith("board:")) {
            handleBoardCallback(callback, data, userId, chatId);
        }

        answerCallback(callback.getId());
    }

    private void handleStatusChange(CallbackQuery callback, String data, Long chatId) {
        // data format: status:<requestId>:<newStatus>
        String[] parts = data.split(":");
        if (parts.length != 3) return;

        Long requestId;
        RequestStatus newStatus;
        try {
            requestId = Long.parseLong(parts[1]);
            newStatus = RequestStatus.valueOf(parts[2]);
        } catch (Exception e) {
            log.warn("Invalid status callback data: {}", data);
            return;
        }

        Optional<Request> requestOpt = requestService.findById(requestId);
        if (requestOpt.isEmpty()) {
            answerCallbackWithText(callback.getId(), "Заявку не знайдено.");
            return;
        }

        Request request = requestOpt.get();
        if (!request.getStatus().nextStatuses().contains(newStatus)) {
            answerCallbackWithText(callback.getId(), "Цей перехід статусу недоступний.");
            return;
        }

        Request updated = requestService.updateStatus(requestId, newStatus);

        Integer messageId = callback.getMessage().getMessageId();
        boolean isAdminNotificationMessage = updated.getAdminChatMessageId() != null
                && updated.getAdminChatMessageId().equals(messageId.longValue());

        if (isAdminNotificationMessage) {
            // Callback came from the original admin notification — let AdminNotificationService handle the edit
            adminNotificationService.updateRequestMessage(updated);
        } else {
            // Callback came from a board detail view — re-render detail in place
            AdminBoardService.BoardMessage boardMsg = adminBoardService.buildDetailMessage(updated, 0, "ALL");
            try {
                telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(boardMsg.text())
                        .parseMode("HTML")
                        .replyMarkup(boardMsg.keyboard())
                        .build());
            } catch (TelegramApiException e) {
                log.warn("Failed to edit board message after status change", e);
            }
        }

        answerCallbackWithText(callback.getId(), "Статус змінено: " + newStatus.getDisplayName());
    }

    private void handleBoardCallback(CallbackQuery callback, String data, Long userId, Long chatId) {
        if (!adminBoardService.isAdmin(userId)) {
            answerCallbackWithText(callback.getId(), "❌ Доступ заборонено.");
            return;
        }

        Integer messageId = callback.getMessage().getMessageId();

        if ("board:noop".equals(data)) {
            // Do nothing, just answer callback
            return;
        }

        // board:v:<requestId>:<returnPage>:<returnFilter>
        if (data.startsWith("board:v:")) {
            String[] parts = data.split(":");
            // parts: [board, v, requestId, returnPage, returnFilter]
            if (parts.length != 5) return;
            try {
                Long requestId = Long.parseLong(parts[2]);
                int returnPage = Integer.parseInt(parts[3]);
                String returnFilter = parts[4];

                Optional<Request> requestOpt = requestService.findById(requestId);
                if (requestOpt.isEmpty()) {
                    answerCallbackWithText(callback.getId(), "Заявку не знайдено.");
                    return;
                }

                AdminBoardService.BoardMessage boardMsg = adminBoardService.buildDetailMessage(requestOpt.get(), returnPage, returnFilter);
                telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .text(boardMsg.text())
                        .parseMode("HTML")
                        .replyMarkup(boardMsg.keyboard())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to handle board view callback: {}", data, e);
            }
            return;
        }

        // board:<page>:<filter>
        String[] parts = data.split(":");
        if (parts.length != 3) return;
        try {
            int page = Integer.parseInt(parts[1]);
            String filter = parts[2];

            AdminBoardService.BoardMessage boardMsg = adminBoardService.buildBoardMessage(page, filter);
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(boardMsg.text())
                    .parseMode("HTML")
                    .replyMarkup(boardMsg.keyboard())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to handle board list callback: {}", data, e);
        }
    }

    private void handleUrgency(CallbackQuery callback, String data, Long userId, Long chatId) {
        Optional<RequestDraft> draftOpt = conversationService.getDraft(userId);
        if (draftOpt.isEmpty() || draftOpt.get().getStep() != ConversationStep.AWAITING_URGENCY) return;

        RequestDraft draft = draftOpt.get();
        draft.setUrgent(data.equals("urgency:yes"));
        draft.setStep(ConversationStep.AWAITING_TYPE);
        messageHandler.sendTypeKeyboardForDraft(chatId);
    }

    private void handleType(CallbackQuery callback, String data, Long userId, Long chatId) {
        Optional<RequestDraft> draftOpt = conversationService.getDraft(userId);
        if (draftOpt.isEmpty() || draftOpt.get().getStep() != ConversationStep.AWAITING_TYPE) return;

        RequestDraft draft = draftOpt.get();
        String typeName = data.substring("type:".length());
        try {
            draft.setType(RequestType.valueOf(typeName));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown request type: {}", typeName);
            return;
        }
        draft.setStep(ConversationStep.AWAITING_DESCRIPTION);
        try {
            telegramClient.execute(
                    SendMessage.builder()
                            .chatId(chatId)
                            .text("Опишіть вашу проблему детально:")
                            .build()
            );
        } catch (TelegramApiException e) {
            log.error("Failed to send message", e);
        }
    }

    private void handleMediaSkip(CallbackQuery callback, Long userId, Long chatId) {
        Optional<RequestDraft> draftOpt = conversationService.getDraft(userId);
        if (draftOpt.isEmpty() || draftOpt.get().getStep() != ConversationStep.AWAITING_MEDIA) return;

        messageHandler.skipMedia(chatId, draftOpt.get());
    }

    private void handleConfirmation(CallbackQuery callback, String data, Long userId, Long chatId) {
        Optional<RequestDraft> draftOpt = conversationService.getDraft(userId);
        if (draftOpt.isEmpty() || draftOpt.get().getStep() != ConversationStep.AWAITING_CONFIRMATION) return;

        if (data.equals("confirm:yes")) {
            messageHandler.confirmAndSubmit(chatId, draftOpt.get());
        } else {
            messageHandler.cancelDraft(chatId, userId);
        }
    }

    private void answerCallback(String callbackId) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder().callbackQueryId(callbackId).build());
        } catch (TelegramApiException e) {
            log.warn("Failed to answer callback {}", callbackId, e);
        }
    }

    private void answerCallbackWithText(String callbackId, String text) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to answer callback {}", callbackId, e);
        }
    }
}
