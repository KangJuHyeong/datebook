package app.dto.diary;

import java.time.LocalDate;

public record DiaryEntryResponse(
        Long dailyQuestionId,
        LocalDate date,
        String question,
        String myAnswerStatus,
        String partnerAnswerStatus,
        DiaryAnswerResponse myAnswer,
        DiaryAnswerResponse partnerAnswer,
        boolean exportable
) {
}
