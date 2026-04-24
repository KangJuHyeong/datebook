package app.domain;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "invite_codes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_invite_codes_code", columnNames = "code")
})
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "couple_id", nullable = false)
    private Couple couple;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_user_id")
    private User usedByUser;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected InviteCode() {
    }

    public InviteCode(Couple couple, String code, LocalDateTime expiresAt) {
        this.couple = couple;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime currentTime) {
        return expiresAt.isBefore(currentTime);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(User user, LocalDateTime usedAt) {
        this.usedByUser = user;
        this.usedAt = usedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public Couple getCouple() {
        return couple;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public User getUsedByUser() {
        return usedByUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
