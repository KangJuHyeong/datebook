package app.dto.export;

public record ExportDownloadLinkResponse(
        String format,
        String url
) {
}
