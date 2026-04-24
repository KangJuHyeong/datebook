package app.dto.diary;

import java.util.List;

public record DiaryResponse(
        List<DiaryEntryResponse> entries
) {
}
