import { apiRequestJson } from "./client";
import type {
  AnswerResponse,
  CreateAnswerRequest,
  TodayQuestionResponse,
  UpdateAnswerRequest,
} from "@/types/api";

export function getTodayQuestion() {
  return apiRequestJson<TodayQuestionResponse>("/api/questions/today", {
    method: "GET",
  });
}

export function createAnswer(data: CreateAnswerRequest) {
  return apiRequestJson<AnswerResponse>("/api/answers", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export function updateAnswer(answerId: number, data: UpdateAnswerRequest) {
  return apiRequestJson<AnswerResponse>(`/api/answers/${answerId}`, {
    method: "PUT",
    body: JSON.stringify(data),
  });
}
