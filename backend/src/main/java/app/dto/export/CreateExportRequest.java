package app.dto.export;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateExportRequest(
        @NotEmpty(message = "주문할 기록을 하나 이상 선택해주세요.")
        List<@NotNull(message = "dailyQuestionId를 확인해주세요.") Long> dailyQuestionIds
) {
}
