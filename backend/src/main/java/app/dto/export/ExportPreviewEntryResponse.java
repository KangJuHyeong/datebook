package app.dto.export;

import java.time.LocalDate;
import java.util.List;

public record ExportPreviewEntryResponse(
        LocalDate date,
        String question,
        List<ExportPreviewAnswerResponse> answers
) {
}
