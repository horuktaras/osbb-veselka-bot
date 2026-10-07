package ua.horuktaras.osbb.bot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ua.horuktaras.osbb.bot.model.dto.RequestDraft;
import ua.horuktaras.osbb.bot.model.entity.Request;
import ua.horuktaras.osbb.bot.model.enums.ConversationStep;
import ua.horuktaras.osbb.bot.model.enums.MediaType;
import ua.horuktaras.osbb.bot.service.AdminNotificationService;
import ua.horuktaras.osbb.bot.service.ConversationService;
import ua.horuktaras.osbb.bot.service.RequestService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);

    private final TelegramClient telegramClient;
    private final ConversationService conversationService;
    private final RequestService requestService;
    private final AdminNotificationService adminNotificationService;

    public MessageHandler(TelegramClient telegramClient,
                          ConversationService conversationService,
                          RequestService requestService,
                          AdminNotificationService adminNotificationService) {
        this.telegramClient = telegramClient;
        this.conversationService = conversationService;
        this.requestService = requestService;
        this.adminNotificationService = adminNotificationService;
    }

    public void handle(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText();

        // Handle commands
        if (text != null && text.startsWith("/")) {
            handleCommand(text, userId, chatId, message);
            return;
        }

        Optional<RequestDraft> draftOpt = conversationService.getDraft(userId);

        if (draftOpt.isEmpty()) {
            startNewDraft(userId, chatId, message);
            return;
        }

        RequestDraft draft = draftOpt.get();

        switch (draft.getStep()) {
            case AWAITING_NAME -> handleName(draft, message);
            case AWAITING_CONTACT -> handlePhone(draft, message);
            case AWAITING_DESCRIPTION -> handleDescription(draft, message);
            case AWAITING_MEDIA -> handleMedia(draft, message);
            default -> send(chatId, "Будь ласка, скористайтесь кнопками нижче.");
        }
    }

    private void handleCommand(String text, Long userId, Long chatId, Message message) {
        String command = text.split(" ")[0].toLowerCase().replaceAll("@.*", "");
        switch (command) {
            case "/start", "/osbb" -> {
                conversationService.removeDraft(userId);
                startNewDraft(userId, chatId, message);
            }
            case "/cancel" -> {
                if (conversationService.hasDraft(userId)) {
                    cancelDraft(chatId, userId);
                } else {
                    send(chatId, "Немає активної заявки для скасування.");
                }
            }
            default -> send(chatId, "Невідома команда. Натисніть /start щоб подати заявку.");
        }
    }

    private void startNewDraft(Long userId, Long chatId, Message message) {
        String username = message.getFrom().getUserName();
        RequestDraft draft = conversationService.startDraft(userId, chatId, username);

        // Auto-fill contact from @username if available
        if (username != null && !username.isBlank()) {
            draft.setContact("@" + username);
        }

        send(chatId, "Вітаємо! Ви можете подати заявку до ОСББ.\n\nЯк вас звати?");
    }

    private void handleName(RequestDraft draft, Message message) {
        String text = message.getText();
        if (text == null || text.isBlank()) {
            send(message.getChatId(), "Будь ласка, введіть ваше ім'я текстом.");
            return;
        }
        draft.setName(text.trim());

        // If @username was already auto-filled — skip phone step
        if (draft.getContact() != null) {
            draft.setStep(ConversationStep.AWAITING_URGENCY);
            sendUrgencyKeyboard(message.getChatId());
        } else {
            draft.setStep(ConversationStep.AWAITING_CONTACT);
            send(message.getChatId(), "У вас немає @username в Telegram.\nВкажіть ваш номер телефону:");
        }
    }

    private void handlePhone(RequestDraft draft, Message message) {
        String text = message.getText();
        if (text == null || text.isBlank()) {
            send(message.getChatId(), "Будь ласка, введіть номер телефону.");
            return;
        }
        draft.setContact(text.trim());
        draft.setStep(ConversationStep.AWAITING_URGENCY);
        sendUrgencyKeyboard(message.getChatId());
    }

    private void handleDescription(RequestDraft draft, Message message) {
        String text = message.getText();
        if (text == null || text.isBlank()) {
            send(message.getChatId(), "Будь ласка, опишіть проблему текстом.");
            return;
        }
        draft.setDescription(text.trim());
        draft.setStep(ConversationStep.AWAITING_MEDIA);
        sendMediaKeyboard(message.getChatId());
    }

    private void handleMedia(RequestDraft draft, Message message) {
        if (message.hasPhoto()) {
            List<PhotoSize> photos = message.getPhoto();
            String fileId = photos.stream()
                    .max(Comparator.comparingInt(PhotoSize::getFileSize))
                    .map(PhotoSize::getFileId)
                    .orElse(null);
            draft.setMediaFileId(fileId);
            draft.setMediaType(MediaType.PHOTO);
        } else if (message.hasVideo()) {
            draft.setMediaFileId(message.getVideo().getFileId());
            draft.setMediaType(MediaType.VIDEO);
        } else {
            send(message.getChatId(), "Будь ласка, надішліть фото, відео або натисніть «Пропустити».");
            return;
        }
        draft.setStep(ConversationStep.AWAITING_CONFIRMATION);
        sendConfirmation(message.getChatId(), draft);
    }

    public void skipMedia(Long chatId, RequestDraft draft) {
        draft.setMediaFileId(null);
        draft.setMediaType(null);
        draft.setStep(ConversationStep.AWAITING_CONFIRMATION);
        sendConfirmation(chatId, draft);
    }

    public void confirmAndSubmit(Long chatId, RequestDraft draft) {
        Request request = new Request();
        request.setTelegramUserId(draft.getUserId());
        request.setSourceChatId(draft.getSourceChatId());
        request.setTelegramUsername(draft.getTelegramUsername());
        request.setName(draft.getName());
        request.setContact(draft.getContact());
        request.setUrgent(draft.getUrgent());
        request.setType(draft.getType());
        request.setDescription(draft.getDescription());
        request.setMediaFileId(draft.getMediaFileId());
        request.setMediaType(draft.getMediaType());

        Request saved = requestService.save(request);

        Integer messageId = adminNotificationService.sendNewRequest(saved);
        if (messageId != null) {
            saved.setAdminChatMessageId(messageId.longValue());
            requestService.save(saved);
        }

        conversationService.removeDraft(draft.getUserId());
        send(chatId, "✅ Вашу заявку <b>#" + saved.getId() + "</b> прийнято!\n\nАдміністрація ОСББ розгляне її найближчим часом.");
    }

    public void cancelDraft(Long chatId, Long userId) {
        conversationService.removeDraft(userId);
        send(chatId, "❌ Заявку скасовано. Надішліть /start щоб подати нову.");
    }

    private void sendUrgencyKeyboard(Long chatId) {
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("❗ Терміново").callbackData("urgency:yes").build(),
                        InlineKeyboardButton.builder().text("Не терміново").callbackData("urgency:no").build()
                ))
                .build();
        sendWithKeyboard(chatId, "Це термінова заявка?", keyboard);
    }

    private void sendTypeKeyboard(Long chatId) {
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("🔧 Поломка").callbackData("type:BREAKAGE").build(),
                        InlineKeyboardButton.builder().text("📣 Скарга").callbackData("type:COMPLAINT").build(),
                        InlineKeyboardButton.builder().text("🛠️ Послуга").callbackData("type:SERVICE").build()
                ))
                .build();
        sendWithKeyboard(chatId, "Оберіть тип заявки:", keyboard);
    }

    private void sendMediaKeyboard(Long chatId) {
        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("Пропустити ➡️").callbackData("media:skip").build()
                ))
                .build();
        sendWithKeyboard(chatId, "Надішліть фото або відео (необов'язково):", keyboard);
    }

    private void sendConfirmation(Long chatId, RequestDraft draft) {
        String urgency = Boolean.TRUE.equals(draft.getUrgent()) ? "❗ Так" : "Ні";
        String media = draft.getMediaFileId() != null ? "✅ Додано" : "Без медіа";

        String text = "<b>Перевірте вашу заявку:</b>\n\n"
                + "👤 Ім'я: " + draft.getName() + "\n"
                + "📞 Контакт: " + draft.getContact() + "\n"
                + "⚡ Терміново: " + urgency + "\n"
                + "🗂 Тип: " + draft.getType().getDisplayName() + "\n"
                + "📝 Опис: " + draft.getDescription() + "\n"
                + "📎 Медіа: " + media;

        InlineKeyboardMarkup keyboard = InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder().text("✅ Підтвердити").callbackData("confirm:yes").build(),
                        InlineKeyboardButton.builder().text("❌ Скасувати").callbackData("confirm:no").build()
                ))
                .build();

        sendWithKeyboard(chatId, text, keyboard);
    }

    public void sendTypeKeyboardForDraft(Long chatId) {
        sendTypeKeyboard(chatId);
    }

    public void send(Long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("HTML")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}", chatId, e);
        }
    }

    private void sendWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(keyboard)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Failed to send message with keyboard to chat {}", chatId, e);
        }
    }
}
