package app.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "export_requests")
public class ExportRequest extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "couple_id", nullable = false)
    private Couple couple;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExportStatus status;

    @Column(name = "previewed_at")
    private LocalDateTime previewedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "json_payload", columnDefinition = "MEDIUMTEXT")
    private String jsonPayload;

    @Column(name = "text_payload", columnDefinition = "MEDIUMTEXT")
    private String textPayload;

    protected ExportRequest() {
    }

    public ExportRequest(Couple couple, User requestedByUser, ExportStatus status) {
        this.couple = couple;
        this.requestedByUser = requestedByUser;
        this.status = status;
    }

    public Couple getCouple() {
        return couple;
    }

    public User getRequestedByUser() {
        return requestedByUser;
    }

    public ExportStatus getStatus() {
        return status;
    }

    public LocalDateTime getPreviewedAt() {
        return previewedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getJsonPayload() {
        return jsonPayload;
    }

    public String getTextPayload() {
        return textPayload;
    }

    public void markPreviewed(LocalDateTime previewedAt, String jsonPayload) {
        this.status = ExportStatus.PREVIEWED;
        this.previewedAt = previewedAt;
        this.jsonPayload = jsonPayload;
    }

    public void markCompleted(LocalDateTime completedAt, String jsonPayload, String textPayload) {
        this.status = ExportStatus.COMPLETED;
        this.completedAt = completedAt;
        this.jsonPayload = jsonPayload;
        this.textPayload = textPayload;
    }

    public void cancel(LocalDateTime cancelledAt) {
        this.status = ExportStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
        this.jsonPayload = null;
        this.textPayload = null;
    }
}
