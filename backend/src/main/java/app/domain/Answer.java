package app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_answers_daily_question_user", columnNames = {"daily_question_id", "user_id"})
})
public class Answer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_question_id", nullable = false)
    private DailyQuestion dailyQuestion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    protected Answer() {
    }

    public Answer(DailyQuestion dailyQuestion, User user, String content) {
        this.dailyQuestion = dailyQuestion;
        this.user = user;
        this.content = content;
    }

    public DailyQuestion getDailyQuestion() {
        return dailyQuestion;
    }

    public User getUser() {
        return user;
    }

    public String getContent() {
        return content;
    }
}
