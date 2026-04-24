export function toggleExportSelection(selectedIds, dailyQuestionId, exportable) {
  if (!exportable) {
    return selectedIds;
  }

  if (selectedIds.includes(dailyQuestionId)) {
    return selectedIds.filter((id) => id !== dailyQuestionId);
  }

  return [...selectedIds, dailyQuestionId];
}

export function getExportEntryViewModel(entry) {
  return {
    dateLabel: new Intl.DateTimeFormat("ko-KR", {
      year: "numeric",
      month: "long",
      day: "numeric",
      weekday: "short",
    }).format(new Date(`${entry.date}T00:00:00`)),
    availabilityBadge: entry.exportable ? "선택 가능" : "잠금 기록",
    availabilityTone: entry.exportable ? "selected" : "waiting",
    availabilityDescription: entry.exportable
      ? "두 사람의 답변이 모두 열려 있어 주문에 담을 수 있어요."
      : "둘 다 답한 기록만 주문할 수 있어요.",
    myAnswerBadge: entry.myAnswerStatus === "ANSWERED" ? "작성 완료" : "미작성",
    myAnswerTone: entry.myAnswerStatus === "ANSWERED" ? "success" : "waiting",
    partnerAnswerBadge: entry.partnerAnswerStatus === "REVEALED" ? "공개됨" : "잠금",
    partnerAnswerTone: entry.partnerAnswerStatus === "REVEALED" ? "success" : "waiting",
  };
}

export async function submitExportSelection(selectedIds, api) {
  if (!selectedIds.length) {
    return {
      ok: false,
      message: "선택한 기록이 없어요. 주문할 기록을 골라주세요.",
    };
  }

  try {
    const order = await api.createExportOrder({ dailyQuestionIds: selectedIds });
    const preview = await api.previewExportOrder(order.exportRequestId);

    return {
      ok: true,
      exportRequestId: order.exportRequestId,
      preview,
    };
  } catch (error) {
    return {
      ok: false,
      message: error instanceof Error ? error.message : "잠시 후 다시 시도해주세요.",
      redirectTo: error?.redirectTo,
      shouldRefetch: Boolean(error?.shouldRefetch),
    };
  }
}

export async function submitExportCompletion(exportRequestId, api) {
  try {
    const completed = await api.completeExportOrder(exportRequestId);

    return {
      ok: true,
      completed,
    };
  } catch (error) {
    return {
      ok: false,
      message: error instanceof Error ? error.message : "잠시 후 다시 시도해주세요.",
      redirectTo: error?.redirectTo,
      shouldRefetch: Boolean(error?.shouldRefetch),
    };
  }
}

export async function cancelExportSelection(exportRequestId, api) {
  try {
    await api.cancelExportOrder(exportRequestId);
    return { ok: true };
  } catch (error) {
    return {
      ok: false,
      message: error instanceof Error ? error.message : "잠시 후 다시 시도해주세요.",
      redirectTo: error?.redirectTo,
      shouldRefetch: Boolean(error?.shouldRefetch),
    };
  }
}
