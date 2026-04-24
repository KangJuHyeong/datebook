package app.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "couple_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_couple_members_user", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_couple_members_couple_user", columnNames = {"couple_id", "user_id"})
})
public class CoupleMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "couple_id", nullable = false)
    private Couple couple;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    protected CoupleMember() {
    }

    public CoupleMember(Couple couple, User user, LocalDateTime joinedAt) {
        this.couple = couple;
        this.user = user;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public Couple getCouple() {
        return couple;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
