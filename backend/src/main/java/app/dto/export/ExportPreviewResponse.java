package app.dto.export;

import java.util.List;

import app.domain.ExportStatus;

public record ExportPreviewResponse(
        Long exportRequestId,
        ExportStatus status,
        List<ExportPreviewEntryResponse> entries
) {
}
