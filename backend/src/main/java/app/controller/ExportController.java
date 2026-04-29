package app.controller;

import app.dto.export.CancelExportResponse;
import app.dto.export.CompleteExportResponse;
import app.dto.export.CreateExportRequest;
import app.dto.export.CreateExportResponse;
import app.dto.export.DeleteExportResponse;
import app.dto.export.ExportOrderDetailResponse;
import app.dto.export.ExportOrderListResponse;
import app.dto.export.ExportPreviewResponse;
import app.service.ExportService;
import app.service.ExportService.DownloadPayload;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping
    public CreateExportResponse createExport(@Valid @RequestBody CreateExportRequest request, HttpSession session) {
        return exportService.createExport(request, session);
    }

    @GetMapping
    public ExportOrderListResponse listExports(HttpSession session) {
        return exportService.listExports(session);
    }

    @GetMapping("/{exportRequestId}")
    public ExportOrderDetailResponse getExport(@PathVariable("exportRequestId") Long exportRequestId, HttpSession session) {
        return exportService.getExport(exportRequestId, session);
    }

    @PostMapping("/{exportRequestId}/preview")
    public ExportPreviewResponse previewExport(@PathVariable("exportRequestId") Long exportRequestId, HttpSession session) {
        return exportService.previewExport(exportRequestId, session);
    }

    @PostMapping("/{exportRequestId}/complete")
    public CompleteExportResponse completeExport(@PathVariable("exportRequestId") Long exportRequestId, HttpSession session) {
        return exportService.completeExport(exportRequestId, session);
    }

    @PostMapping("/{exportRequestId}/cancel")
    public CancelExportResponse cancelExport(@PathVariable("exportRequestId") Long exportRequestId, HttpSession session) {
        return exportService.cancelExport(exportRequestId, session);
    }

    @DeleteMapping("/{exportRequestId}")
    public DeleteExportResponse deleteExport(@PathVariable("exportRequestId") Long exportRequestId, HttpSession session) {
        return exportService.deleteExport(exportRequestId, session);
    }

    @GetMapping("/{exportRequestId}/download")
    public ResponseEntity<String> downloadExport(
            @PathVariable("exportRequestId") Long exportRequestId,
            @RequestParam("format") String format,
            HttpSession session
    ) {
        DownloadPayload payload = exportService.downloadExport(exportRequestId, format, session);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, payload.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(payload.fileName())
                        .build()
                        .toString())
                .body(payload.body());
    }
}
