package app.dto.answer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAnswerRequest(
        @NotBlank(message = "답변을 입력해주세요.")
        @Size(min = 1, max = 2000, message = "답변은 1자 이상 2000자 이하로 입력해주세요.")
        String content
) {
}
