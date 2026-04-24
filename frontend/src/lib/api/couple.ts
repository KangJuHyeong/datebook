import { apiRequestJson } from "./client";
import type {
  CreateCoupleResponse,
  JoinCoupleRequest,
  JoinCoupleResponse,
} from "@/types/api";

export function createCouple() {
  return apiRequestJson<CreateCoupleResponse>("/api/couples", {
    method: "POST",
  });
}

export function joinCouple(data: JoinCoupleRequest) {
  return apiRequestJson<JoinCoupleResponse>("/api/couples/join", {
    method: "POST",
    body: JSON.stringify(data),
  });
}
