package app.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "daily_questions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_questions_couple_date", columnNames = {"couple_id", "question_date"})
})
public class DailyQuestion extends CreatedAtEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "couple_id", nullable = false)
    private Couple couple;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "question_date", nullable = false)
    private LocalDate questionDate;

    protected DailyQuestion() {
    }

    public DailyQuestion(Couple couple, Question question, LocalDate questionDate) {
        this.couple = couple;
        this.question = question;
        this.questionDate = questionDate;
    }

    public Couple getCouple() {
        return couple;
    }

    public Question getQuestion() {
        return question;
    }

    public LocalDate getQuestionDate() {
        return questionDate;
    }
}
