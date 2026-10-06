package ua.horuktaras.osbb.bot.model.entity;

import jakarta.persistence.*;
import ua.horuktaras.osbb.bot.model.enums.WarningPunishment;

import java.time.Instant;

@Entity
@Table(name = "chat_configs")
public class ChatConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    @Column(name = "verification_enabled", nullable = false)
    private boolean verificationEnabled = true;

    @Column(name = "verification_timeout_seconds", nullable = false)
    private int verificationTimeoutSeconds = 600;

    @Column(name = "anti_link_enabled", nullable = false)
    private boolean antiLinkEnabled = true;

    @Column(name = "anti_spam_enabled", nullable = false)
    private boolean antiSpamEnabled = true;

    @Column(name = "max_messages_per_window", nullable = false)
    private int maxMessagesPerWindow = 10;

    @Column(name = "rate_limit_window_seconds", nullable = false)
    private int rateLimitWindowSeconds = 60;

    @Column(name = "warning_limit", nullable = false)
    private int warningLimit = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_punishment", nullable = false, length = 50)
    private WarningPunishment warningPunishment = WarningPunishment.MUTE;

    @Column(name = "warning_punishment_duration_seconds", nullable = false)
    private int warningPunishmentDurationSeconds = 86400;

    @Column(name = "allowed_domains", columnDefinition = "TEXT")
    private String allowedDomains;

    @Column(name = "blacklisted_keywords", columnDefinition = "TEXT")
    private String blacklistedKeywords;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public boolean isVerificationEnabled() { return verificationEnabled; }
    public void setVerificationEnabled(boolean verificationEnabled) { this.verificationEnabled = verificationEnabled; }

    public int getVerificationTimeoutSeconds() { return verificationTimeoutSeconds; }
    public void setVerificationTimeoutSeconds(int verificationTimeoutSeconds) { this.verificationTimeoutSeconds = verificationTimeoutSeconds; }

    public boolean isAntiLinkEnabled() { return antiLinkEnabled; }
    public void setAntiLinkEnabled(boolean antiLinkEnabled) { this.antiLinkEnabled = antiLinkEnabled; }

    public boolean isAntiSpamEnabled() { return antiSpamEnabled; }
    public void setAntiSpamEnabled(boolean antiSpamEnabled) { this.antiSpamEnabled = antiSpamEnabled; }

    public int getMaxMessagesPerWindow() { return maxMessagesPerWindow; }
    public void setMaxMessagesPerWindow(int maxMessagesPerWindow) { this.maxMessagesPerWindow = maxMessagesPerWindow; }

    public int getRateLimitWindowSeconds() { return rateLimitWindowSeconds; }
    public void setRateLimitWindowSeconds(int rateLimitWindowSeconds) { this.rateLimitWindowSeconds = rateLimitWindowSeconds; }

    public int getWarningLimit() { return warningLimit; }
    public void setWarningLimit(int warningLimit) { this.warningLimit = warningLimit; }

    public WarningPunishment getWarningPunishment() { return warningPunishment; }
    public void setWarningPunishment(WarningPunishment warningPunishment) { this.warningPunishment = warningPunishment; }

    public int getWarningPunishmentDurationSeconds() { return warningPunishmentDurationSeconds; }
    public void setWarningPunishmentDurationSeconds(int warningPunishmentDurationSeconds) { this.warningPunishmentDurationSeconds = warningPunishmentDurationSeconds; }

    public String getAllowedDomains() { return allowedDomains; }
    public void setAllowedDomains(String allowedDomains) { this.allowedDomains = allowedDomains; }

    public String getBlacklistedKeywords() { return blacklistedKeywords; }
    public void setBlacklistedKeywords(String blacklistedKeywords) { this.blacklistedKeywords = blacklistedKeywords; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
