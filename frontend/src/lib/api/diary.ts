import { apiRequestJson } from "./client";
import type { DiaryResponse } from "@/types/api";

export function getDiaryEntries() {
  return apiRequestJson<DiaryResponse>("/api/diary", {
    method: "GET",
  });
}
