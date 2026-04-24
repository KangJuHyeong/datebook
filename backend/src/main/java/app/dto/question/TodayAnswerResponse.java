package app.dto.question;

import java.time.LocalDateTime;

public record TodayAnswerResponse(
        Long id,
        String content,
        LocalDateTime updatedAt
) {
}
