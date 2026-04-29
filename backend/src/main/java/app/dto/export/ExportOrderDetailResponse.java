package app.dto.export;

import java.time.LocalDateTime;
import java.util.List;

import app.domain.ExportStatus;

public record ExportOrderDetailResponse(
        Long exportRequestId,
        ExportStatus status,
        int itemCount,
        LocalDateTime createdAt,
        LocalDateTime previewedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        List<ExportPreviewEntryResponse> entries,
        List<ExportDownloadLinkResponse> downloads
) {
}
