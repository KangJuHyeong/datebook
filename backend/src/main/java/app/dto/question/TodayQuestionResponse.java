package app.dto.question;

import java.time.LocalDate;

import app.domain.AnswerState;

public record TodayQuestionResponse(
        Long dailyQuestionId,
        LocalDate date,
        AnswerState answerState,
        String question,
        TodayAnswerResponse myAnswer,
        PartnerAnswerResponse partnerAnswer,
        boolean isFullyAnswered
) {
}
