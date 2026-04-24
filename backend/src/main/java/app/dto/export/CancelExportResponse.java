package app.dto.export;

import app.domain.ExportStatus;

public record CancelExportResponse(
        Long exportRequestId,
        ExportStatus status
) {
}
