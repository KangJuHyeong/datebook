import { apiRequestJson } from "./client";
import type { TodayQuestionResponse } from "@/types/api";

export function getTodayQuestion() {
  return apiRequestJson<TodayQuestionResponse>("/api/questions/today", {
    method: "GET",
  });
}
