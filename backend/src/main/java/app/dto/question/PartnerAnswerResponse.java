package app.dto.question;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import app.domain.PartnerAnswerStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PartnerAnswerResponse(
        PartnerAnswerStatus status,
        Long id,
        String content,
        LocalDateTime updatedAt
) {
}
