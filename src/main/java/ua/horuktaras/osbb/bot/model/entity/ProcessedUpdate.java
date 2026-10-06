package ua.horuktaras.osbb.bot.model.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "processed_updates")
public class ProcessedUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "update_id", nullable = false, unique = true)
    private Integer updateId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (this.processedAt == null) {
            this.processedAt = Instant.now();
        }
    }

    public ProcessedUpdate() {}

    public ProcessedUpdate(Integer updateId) {
        this.updateId = updateId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUpdateId() { return updateId; }
    public void setUpdateId(Integer updateId) { this.updateId = updateId; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
