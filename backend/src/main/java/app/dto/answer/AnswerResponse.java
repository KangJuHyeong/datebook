package app.dto.answer;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long id,
        Long dailyQuestionId,
        String content,
        LocalDateTime updatedAt
) {
}
