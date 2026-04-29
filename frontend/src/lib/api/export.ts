import { apiFetch, apiRequestJson } from "./client";
import type {
  CancelExportResponse,
  CompleteExportResponse,
  CreateExportRequest,
  CreateExportResponse,
  DeleteExportResponse,
  ExportOrderDetailResponse,
  ExportOrderListResponse,
  ExportPreviewResponse,
} from "@/types/api";

export function getExportOrders() {
  return apiRequestJson<ExportOrderListResponse>("/api/exports", {
    method: "GET",
  });
}

export function getExportOrderDetail(exportRequestId: number) {
  return apiRequestJson<ExportOrderDetailResponse>(`/api/exports/${exportRequestId}`, {
    method: "GET",
  });
}

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

export function deleteExportOrder(exportRequestId: number) {
  return apiRequestJson<DeleteExportResponse>(`/api/exports/${exportRequestId}`, {
    method: "DELETE",
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
