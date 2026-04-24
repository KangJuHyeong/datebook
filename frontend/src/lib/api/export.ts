import { apiFetch, apiRequestJson } from "./client";
import type {
  CancelExportResponse,
  CompleteExportResponse,
  CreateExportRequest,
  CreateExportResponse,
  ExportPreviewResponse,
} from "@/types/api";

export function createExportOrder(data: CreateExportRequest) {
  return apiRequestJson<CreateExportResponse>("/api/exports", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export function previewExportOrder(exportRequestId: number) {
  return apiRequestJson<ExportPreviewResponse>(`/api/exports/${exportRequestId}/preview`, {
    method: "POST",
  });
}

export function completeExportOrder(exportRequestId: number) {
  return apiRequestJson<CompleteExportResponse>(`/api/exports/${exportRequestId}/complete`, {
    method: "POST",
  });
}

export function cancelExportOrder(exportRequestId: number) {
  return apiRequestJson<CancelExportResponse>(`/api/exports/${exportRequestId}/cancel`, {
    method: "POST",
  });
}

export function downloadExportOrder(exportRequestId: number, format: "json" | "text") {
  return apiFetch(`/api/exports/${exportRequestId}/download?format=${format}`, {
    method: "GET",
    headers: {
      Accept: format === "json" ? "application/json" : "text/plain",
    },
  });
}
