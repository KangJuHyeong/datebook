export type ApiFieldError = {
  field: string;
  message: string;
};

export type ApiErrorCode =
  | "AUTH_REQUIRED"
  | "LOGIN_FAILED"
  | "FORBIDDEN"
  | "NOT_FOUND"
  | "VALIDATION_ERROR"
  | "INVITE_CODE_INVALID"
  | "COUPLE_FULL"
  | "ALREADY_IN_COUPLE"
  | "DAILY_QUESTION_CONFLICT"
  | "ANSWER_LOCKED"
  | "ANSWER_NOT_OWNED"
  | "EXPORT_ITEM_INVALID"
  | "EXPORT_STATE_INVALID"
  | "EXPORT_NOT_COMPLETED"
  | "EXPORT_FORMAT_INVALID"
  | "CONFIGURATION_ERROR"
  | "INTERNAL_ERROR";

export type ApiErrorResponse = {
  code: ApiErrorCode | string;
  message: string;
  fields: ApiFieldError[];
};

export type AuthUser = {
  id: number;
  email: string;
  displayName: string;
  coupleId: number | null;
};

export type SignupRequest = {
  email: string;
  password: string;
  displayName: string;
};

export type SignupResponse = {
  id: number;
  email: string;
  displayName: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type LogoutResponse = {
  success: boolean;
};

export type CreateCoupleResponse = {
  coupleId: number;
  inviteCode: string;
  expiresAt: string;
};

export type JoinCoupleRequest = {
  inviteCode: string;
};

export type JoinCoupleResponse = {
  coupleId: number;
  memberCount: number;
};

export type AnswerState =
  | "NOT_ANSWERED"
  | "MY_ANSWERED_PARTNER_WAITING"
  | "PARTNER_ANSWERED_ME_WAITING"
  | "BOTH_ANSWERED";

export type PartnerAnswerStatus = "LOCKED" | "ANSWERED_LOCKED" | "REVEALED";

export type TodayAnswer = {
  id: number;
  content: string;
  updatedAt: string;
};

export type AnswerResponse = TodayAnswer & {
  dailyQuestionId: number;
};

export type CreateAnswerRequest = {
  dailyQuestionId: number;
  content: string;
};

export type UpdateAnswerRequest = {
  content: string;
};

export type PartnerAnswer = {
  status: PartnerAnswerStatus;
  id?: number;
  content?: string;
  updatedAt?: string;
};

export type TodayQuestionResponse = {
  dailyQuestionId: number;
  date: string;
  answerState: AnswerState;
  question: string;
  myAnswer: TodayAnswer | null;
  partnerAnswer: PartnerAnswer;
  isFullyAnswered: boolean;
};

export type DiaryEntry = {
  dailyQuestionId: number;
  date: string;
  question: string;
  myAnswerStatus: string;
  partnerAnswerStatus: string;
  exportable: boolean;
};

export type DiaryResponse = {
  entries: DiaryEntry[];
};

export type ExportStatus =
  | "REQUESTED"
  | "PREVIEWED"
  | "COMPLETED"
  | "CANCELLED";

export type CreateExportRequest = {
  dailyQuestionIds: number[];
};

export type CreateExportResponse = {
  exportRequestId: number;
  status: ExportStatus;
  itemCount: number;
};

export type ExportPreviewAnswer = {
  displayName: string;
  content: string;
};

export type ExportPreviewEntry = {
  date: string;
  question: string;
  answers: ExportPreviewAnswer[];
};

export type ExportPreviewResponse = {
  exportRequestId: number;
  status: ExportStatus;
  entries: ExportPreviewEntry[];
};

export type ExportDownloadLink = {
  format: string;
  url: string;
};

export type CompleteExportResponse = {
  exportRequestId: number;
  status: ExportStatus;
  completedAt: string;
  downloads: ExportDownloadLink[];
};

export type CancelExportResponse = {
  exportRequestId: number;
  status: ExportStatus;
};
