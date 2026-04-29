package app.dto.export;

public record DeleteExportResponse(
        Long exportRequestId,
        boolean deleted
) {
}
