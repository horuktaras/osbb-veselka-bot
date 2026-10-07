package ua.horuktaras.osbb.bot.model.dto;

import ua.horuktaras.osbb.bot.model.enums.ConversationStep;
import ua.horuktaras.osbb.bot.model.enums.MediaType;
import ua.horuktaras.osbb.bot.model.enums.RequestType;

public class RequestDraft {

    private Long userId;
    private Long sourceChatId;
    private String telegramUsername;
    private ConversationStep step;
    private String name;
    private String contact;
    private Boolean urgent;
    private RequestType type;
    private String description;
    private String mediaFileId;
    private MediaType mediaType;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSourceChatId() { return sourceChatId; }
    public void setSourceChatId(Long sourceChatId) { this.sourceChatId = sourceChatId; }

    public String getTelegramUsername() { return telegramUsername; }
    public void setTelegramUsername(String telegramUsername) { this.telegramUsername = telegramUsername; }

    public ConversationStep getStep() { return step; }
    public void setStep(ConversationStep step) { this.step = step; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public Boolean getUrgent() { return urgent; }
    public void setUrgent(Boolean urgent) { this.urgent = urgent; }

    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMediaFileId() { return mediaFileId; }
    public void setMediaFileId(String mediaFileId) { this.mediaFileId = mediaFileId; }

    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
}
