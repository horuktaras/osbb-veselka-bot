package ua.horuktaras.osbb.bot.model.entity;

import jakarta.persistence.*;
import ua.horuktaras.osbb.bot.model.enums.ModerationAction;

import java.time.Instant;

@Entity
@Table(name = "moderation_logs")
public class ModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "admin_telegram_user_id")
    private Long adminTelegramUserId;

    @Column(name = "target_telegram_user_id", nullable = false)
    private Long targetTelegramUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 100)
    private ModerationAction action;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public Long getAdminTelegramUserId() { return adminTelegramUserId; }
    public void setAdminTelegramUserId(Long adminTelegramUserId) { this.adminTelegramUserId = adminTelegramUserId; }

    public Long getTargetTelegramUserId() { return targetTelegramUserId; }
    public void setTargetTelegramUserId(Long targetTelegramUserId) { this.targetTelegramUserId = targetTelegramUserId; }

    public ModerationAction getAction() { return action; }
    public void setAction(ModerationAction action) { this.action = action; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
