package ua.horuktaras.osbb.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.config.BotProperties;
import ua.horuktaras.osbb.bot.model.entity.Request;
import ua.horuktaras.osbb.bot.model.enums.RequestStatus;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final TelegramClient telegramClient;
    private final BotProperties botProperties;

    public AdminNotificationService(TelegramClient telegramClient, BotProperties botProperties) {
        this.telegramClient = telegramClient;
        this.botProperties = botProperties;
    }

    public Integer sendNewRequest(Request request) {
        String text = formatRequest(request);
        InlineKeyboardMarkup keyboard = buildKeyboard(request);
        String adminChatId = botProperties.adminChatId();

        try {
            Message sent;
            if (request.getMediaFileId() != null) {
                // Send media with caption, then we'll store this message ID
                if (request.getMediaType() == ua.horuktaras.osbb.bot.model.enums.MediaType.PHOTO) {
                    SendPhoto sendPhoto = SendPhoto.builder()
                            .chatId(adminChatId)
                            .photo(new InputFile(request.getMediaFileId()))
                            .caption(text)
                            .parseMode("HTML")
                            .replyMarkup(keyboard)
                            .build();
                    sent = telegramClient.execute(sendPhoto);
                } else {
                    SendVideo sendVideo = SendVideo.builder()
                            .chatId(adminChatId)
                            .video(new InputFile(request.getMediaFileId()))
                            .caption(text)
                            .parseMode("HTML")
                            .replyMarkup(keyboard)
                            .build();
                    sent = telegramClient.execute(sendVideo);
                }
            } else {
                SendMessage sendMessage = SendMessage.builder()
                        .chatId(adminChatId)
                        .text(text)
                        .parseMode("HTML")
                        .replyMarkup(keyboard)
                        .build();
                sent = telegramClient.execute(sendMessage);
            }
            return sent.getMessageId();
        } catch (TelegramApiException e) {
            log.error("Failed to send request #{} to admin chat", request.getId(), e);
            return null;
        }
    }

    public void updateRequestMessage(Request request) {
        String adminChatId = botProperties.adminChatId();
        Integer messageId = request.getAdminChatMessageId().intValue();
        InlineKeyboardMarkup keyboard = buildKeyboard(request);

        try {
            if (request.getMediaFileId() != null) {
                EditMessageReplyMarkup editMarkup = EditMessageReplyMarkup.builder()
                        .chatId(adminChatId)
                        .messageId(messageId)
                        .replyMarkup(keyboard)
                        .build();
                telegramClient.execute(editMarkup);
            } else {
                EditMessageText editText = EditMessageText.builder()
                        .chatId(adminChatId)
                        .messageId(messageId)
                        .text(formatRequest(request))
                        .parseMode("HTML")
                        .replyMarkup(keyboard)
                        .build();
                telegramClient.execute(editText);
            }
        } catch (TelegramApiException e) {
            log.error("Failed to update request #{} message in admin chat", request.getId(), e);
        }

        notifyResident(request);
    }

    private void notifyResident(Request request) {
        if (request.getSourceChatId() == null) return;

        String statusLine = request.getStatus().getDisplayName();
        String text = "📋 Статус вашої заявки <b>#" + request.getId() + "</b> оновлено:\n\n"
                + statusLine;

        if (request.getStatusComment() != null) {
            text += "\n\n💬 " + escapeHtml(request.getStatusComment());
        }

        if (request.getStatus() == RequestStatus.CLOSED) {
            text += "\n\nДякуємо за звернення до ОСББ!";
        }

        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(request.getSourceChatId())
                    .text(text)
                    .parseMode("HTML")
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to notify resident for request #{}", request.getId(), e);
        }
    }

    private String formatRequest(Request request) {
        String urgencyMark = request.isUrgent() ? "❗ Так" : "Ні";
        String userMention = request.getTelegramUsername() != null
                ? "@" + request.getTelegramUsername()
                : "id:" + request.getTelegramUserId();

        return "<b>📋 Заявка #" + request.getId() + "</b>\n\n"
                + "👤 <b>Ім'я:</b> " + escapeHtml(request.getName()) + "\n"
                + "📞 <b>Контакт:</b> " + escapeHtml(request.getContact()) + "\n"
                + "⚡ <b>Терміново:</b> " + urgencyMark + "\n"
                + "🗂 <b>Тип:</b> " + request.getType().getDisplayName() + "\n"
                + "📝 <b>Опис:</b> " + escapeHtml(request.getDescription()) + "\n"
                + "🙍 <b>Від:</b> " + userMention + "\n"
                + "📅 <b>Подано:</b> " + request.getCreatedAt().format(FORMATTER) + "\n\n"
                + "Статус: " + request.getStatus().getDisplayName()
                + (request.getStatusComment() != null ? "\n💬 <b>Коментар:</b> " + escapeHtml(request.getStatusComment()) : "");
    }

    private InlineKeyboardMarkup buildKeyboard(Request request) {
        List<RequestStatus> nextStatuses = request.getStatus().nextStatuses();
        if (nextStatuses.isEmpty()) {
            return InlineKeyboardMarkup.builder().build();
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        List<InlineKeyboardButton> currentRow = new ArrayList<>();

        for (int i = 0; i < nextStatuses.size(); i++) {
            RequestStatus status = nextStatuses.get(i);
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(status.getDisplayName())
                    .callbackData("status:" + request.getId() + ":" + status.name())
                    .build();
            currentRow.add(button);

            // Max 2 buttons per row
            if (currentRow.size() == 2 || i == nextStatuses.size() - 1) {
                rows.add(new InlineKeyboardRow(currentRow));
                currentRow = new ArrayList<>();
            }
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
