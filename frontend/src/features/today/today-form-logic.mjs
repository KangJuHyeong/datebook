import { ApiError } from "../../lib/api/runtime.mjs";

export const ANSWER_MAX_LENGTH = 2000;

export const TODAY_STATE_COPY = {
  NOT_ANSWERED: "오늘의 답변을 남겨보세요.",
  MY_ANSWERED_PARTNER_WAITING: "내 답변은 저장됐어요. 상대가 답하면 함께 열려요.",
  PARTNER_ANSWERED_ME_WAITING: "상대가 답변을 마쳤어요. 내 답변을 남기면 함께 열려요.",
  BOTH_ANSWERED: "두 사람의 답변이 모두 열렸어요.",
};

export function validateAnswerContent(content) {
  const normalized = typeof content === "string" ? content : "";

  if (!normalized.trim()) {
    return "답변을 입력해주세요.";
  }

  if (normalized.length > ANSWER_MAX_LENGTH) {
    return "답변은 2000자 이하로 입력해주세요.";
  }

  return undefined;
}

export function getTodayStatusCopy(answerState) {
  return TODAY_STATE_COPY[answerState] ?? TODAY_STATE_COPY.NOT_ANSWERED;
}

export function getTodayViewModel(todayQuestion) {
  const statusCopy = getTodayStatusCopy(todayQuestion.answerState);
  const isRevealed = todayQuestion.partnerAnswer.status === "REVEALED";
  const hasMyAnswer = Boolean(todayQuestion.myAnswer);

  return {
    statusCopy,
    questionDateLabel: `${todayQuestion.date}의 질문`,
    myAnswer: {
      badge: hasMyAnswer ? "작성 완료" : "아직 작성 전",
      tone: hasMyAnswer ? "success" : "neutral",
      description: hasMyAnswer
        ? "내 답변은 저장된 내용으로 기준이 잡혀 있어요. 필요하면 다시 수정할 수 있어요."
        : "저장 버튼을 누르면 오늘의 답변이 기록돼요.",
    },
    partnerAnswer: isRevealed
      ? {
          badge: "공개됨",
          tone: "success",
          description: "두 사람 모두 답변을 남겨서 상대 답변이 공개됐어요.",
          content: todayQuestion.partnerAnswer.content ?? "",
          updatedAt: todayQuestion.partnerAnswer.updatedAt,
        }
      : todayQuestion.partnerAnswer.status === "ANSWERED_LOCKED"
        ? {
            badge: "답변 완료",
            tone: "progress",
            description: TODAY_STATE_COPY.PARTNER_ANSWERED_ME_WAITING,
            content: undefined,
          }
        : {
            badge: "잠금",
            tone: "waiting",
            description: "상대가 답하면 열려요.",
            content: undefined,
          },
  };
}

function mapFieldErrors(fields = []) {
  return fields.reduce((accumulator, field) => {
    if (field?.field && field?.message) {
      accumulator[field.field] = field.message;
    }

    return accumulator;
  }, {});
}

export async function submitTodayAnswer({ todayQuestion, content }, dependencies) {
  const validationError = validateAnswerContent(content);

  if (validationError) {
    return {
      ok: false,
      fieldErrors: { content: validationError },
      formError: undefined,
    };
  }

  try {
    if (todayQuestion.myAnswer?.id) {
      await dependencies.updateAnswer(todayQuestion.myAnswer.id, { content });
    } else {
      await dependencies.createAnswer({
        dailyQuestionId: todayQuestion.dailyQuestionId,
        content,
      });
    }

    const refreshedTodayQuestion = await dependencies.getTodayQuestion();

    return {
      ok: true,
      todayQuestion: refreshedTodayQuestion,
      successMessage: "저장됐어요.",
    };
  } catch (error) {
    if (error instanceof ApiError) {
      return {
        ok: false,
        fieldErrors: mapFieldErrors(error.fields),
        formError: error.message,
        redirectTo: error.redirectTo,
        shouldRefetch: error.shouldRefetch,
      };
    }

    return {
      ok: false,
      fieldErrors: {},
      formError: "잠시 후 다시 시도해주세요.",
    };
  }
}
