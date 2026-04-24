package app.dto.export;

import app.domain.ExportStatus;

public record CreateExportResponse(
        Long exportRequestId,
        ExportStatus status,
        int itemCount
) {
}
