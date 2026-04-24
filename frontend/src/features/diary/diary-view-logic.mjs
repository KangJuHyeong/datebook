export function getDiaryEntryViewModel(entry) {
  const myAnswered = entry.myAnswerStatus === "ANSWERED";
  const partnerStatusCopy =
    entry.partnerAnswerStatus === "REVEALED"
      ? "두 사람의 답변이 모두 공개됐어요."
      : entry.partnerAnswerStatus === "ANSWERED_LOCKED"
        ? "상대가 답변을 마쳤어요. 내 답변을 남기면 함께 열려요."
        : "상대가 답하면 열려요.";

  return {
    dateLabel: entry.date,
    myAnswerBadge: myAnswered ? "작성 완료" : "아직 작성 전",
    myAnswerTone: myAnswered ? "success" : "neutral",
    partnerAnswerBadge:
      entry.partnerAnswerStatus === "REVEALED"
        ? "공개됨"
        : entry.partnerAnswerStatus === "ANSWERED_LOCKED"
          ? "답변 완료"
          : "잠금",
    partnerAnswerTone:
      entry.partnerAnswerStatus === "REVEALED"
        ? "success"
        : entry.partnerAnswerStatus === "ANSWERED_LOCKED"
          ? "progress"
          : "waiting",
    partnerStatusCopy,
    exportableCopy: entry.exportable ? "주문에 담을 수 있어요." : "둘 다 답한 기록만 주문할 수 있어요.",
  };
}
