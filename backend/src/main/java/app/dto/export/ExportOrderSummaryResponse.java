package app.dto.export;

import java.time.LocalDateTime;

import app.domain.ExportStatus;

public record ExportOrderSummaryResponse(
        Long exportRequestId,
        ExportStatus status,
        int itemCount,
        LocalDateTime createdAt,
        LocalDateTime previewedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt
) {
}
