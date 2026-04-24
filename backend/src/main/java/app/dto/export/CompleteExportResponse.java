package app.dto.export;

import java.time.LocalDateTime;
import java.util.List;

import app.domain.ExportStatus;

public record CompleteExportResponse(
        Long exportRequestId,
        ExportStatus status,
        LocalDateTime completedAt,
        List<ExportDownloadLinkResponse> downloads
) {
}
