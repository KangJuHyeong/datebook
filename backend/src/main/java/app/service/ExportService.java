package app.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
import app.common.time.AppTimeProvider;
import app.domain.Answer;
import app.domain.CoupleMember;
import app.domain.DailyQuestion;
import app.domain.ExportItem;
import app.domain.ExportRequest;
import app.domain.ExportStatus;
import app.domain.User;
import app.dto.export.CancelExportResponse;
import app.dto.export.CompleteExportResponse;
import app.dto.export.CreateExportRequest;
import app.dto.export.CreateExportResponse;
import app.dto.export.DeleteExportResponse;
import app.dto.export.ExportDownloadLinkResponse;
import app.dto.export.ExportOrderDetailResponse;
import app.dto.export.ExportOrderListResponse;
import app.dto.export.ExportOrderSummaryResponse;
import app.dto.export.ExportPreviewAnswerResponse;
import app.dto.export.ExportPreviewEntryResponse;
import app.dto.export.ExportPreviewResponse;
import app.repository.AnswerRepository;
import app.repository.CoupleMemberRepository;
import app.repository.DailyQuestionRepository;
import app.repository.ExportItemRepository;
import app.repository.ExportRequestRepository;
import app.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExportService {

    private final UserRepository userRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final DailyQuestionRepository dailyQuestionRepository;
    private final AnswerRepository answerRepository;
    private final ExportRequestRepository exportRequestRepository;
    private final ExportItemRepository exportItemRepository;
    private final AppTimeProvider appTimeProvider;
    private final ObjectMapper objectMapper;

    public ExportService(
            UserRepository userRepository,
            CoupleMemberRepository coupleMemberRepository,
            DailyQuestionRepository dailyQuestionRepository,
            AnswerRepository answerRepository,
            ExportRequestRepository exportRequestRepository,
            ExportItemRepository exportItemRepository,
            AppTimeProvider appTimeProvider,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.coupleMemberRepository = coupleMemberRepository;
        this.dailyQuestionRepository = dailyQuestionRepository;
        this.answerRepository = answerRepository;
        this.exportRequestRepository = exportRequestRepository;
        this.exportItemRepository = exportItemRepository;
        this.appTimeProvider = appTimeProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateExportResponse createExport(CreateExportRequest request, HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        Set<Long> dailyQuestionIds = sanitizeDailyQuestionIds(request.dailyQuestionIds());
        List<DailyQuestion> dailyQuestions = loadValidExportDailyQuestions(coupleMember, dailyQuestionIds);

        ExportRequest exportRequest = exportRequestRepository.saveAndFlush(
                new ExportRequest(coupleMember.getCouple(), coupleMember.getUser(), ExportStatus.REQUESTED)
        );

        for (int index = 0; index < dailyQuestions.size(); index++) {
            exportItemRepository.save(new ExportItem(exportRequest, dailyQuestions.get(index), index));
        }
        exportItemRepository.flush();

        return new CreateExportResponse(exportRequest.getId(), exportRequest.getStatus(), dailyQuestions.size());
    }

    public ExportOrderListResponse listExports(HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        List<ExportRequest> exportRequests = exportRequestRepository.findAllByCouple_IdAndStatusInOrderByCreatedAtDesc(
                coupleMember.getCouple().getId(),
                List.of(ExportStatus.PREVIEWED, ExportStatus.COMPLETED)
        );
        Map<Long, Integer> itemCountsByExportRequestId = loadItemCounts(exportRequests);
        List<ExportOrderSummaryResponse> orders = exportRequests.stream()
                .map(exportRequest -> toOrderSummary(exportRequest, itemCountsByExportRequestId))
                .toList();

        return new ExportOrderListResponse(orders);
    }

    public ExportOrderDetailResponse getExport(Long exportRequestId, HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        ExportRequest exportRequest = getOwnedExportRequest(exportRequestId, coupleMember);
        List<ExportPreviewEntryResponse> entries = null;
        List<ExportDownloadLinkResponse> downloads = List.of();

        if (exportRequest.getStatus() == ExportStatus.PREVIEWED) {
            entries = readSnapshotEntries(exportRequest);
        } else if (exportRequest.getStatus() == ExportStatus.COMPLETED) {
            entries = readSnapshotEntries(exportRequest);
            downloads = buildDownloadLinks(exportRequest);
        }

        return new ExportOrderDetailResponse(
                exportRequest.getId(),
                exportRequest.getStatus(),
                exportItemRepository.countByExportRequest_Id(exportRequest.getId()),
                exportRequest.getCreatedAt(),
                exportRequest.getPreviewedAt(),
                exportRequest.getCompletedAt(),
                exportRequest.getCancelledAt(),
                entries,
                downloads
        );
    }

    @Transactional
    public ExportPreviewResponse previewExport(Long exportRequestId, HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        ExportRequest exportRequest = getOwnedExportRequest(exportRequestId, coupleMember);
        if (exportRequest.getStatus() != ExportStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.EXPORT_STATE_INVALID);
        }

        List<ExportPreviewEntryResponse> entries = buildPreviewEntries(exportRequest, coupleMember.getCouple().getId());
        LocalDateTime previewedAt = appTimeProvider.nowUtcDateTime();
        exportRequest.markPreviewed(previewedAt, buildJsonPayload(exportRequest, entries, previewedAt));

        return new ExportPreviewResponse(exportRequest.getId(), exportRequest.getStatus(), entries);
    }

    @Transactional
    public CompleteExportResponse completeExport(Long exportRequestId, HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        ExportRequest exportRequest = getOwnedExportRequest(exportRequestId, coupleMember);
        if (exportRequest.getStatus() != ExportStatus.PREVIEWED) {
            throw new BusinessException(ErrorCode.EXPORT_STATE_INVALID);
        }

        List<ExportPreviewEntryResponse> entries = readSnapshotEntries(exportRequest);
        LocalDateTime completedAt = appTimeProvider.nowUtcDateTime();
        String jsonPayload = buildJsonPayload(exportRequest, entries, completedAt);
        String textPayload = buildTextPayload(entries);

        exportRequest.markCompleted(completedAt, jsonPayload, textPayload);

        return new CompleteExportResponse(
                exportRequest.getId(),
                exportRequest.getStatus(),
                exportRequest.getCompletedAt(),
                buildDownloadLinks(exportRequest)
        );
    }

    @Transactional
    public CancelExportResponse cancelExport(Long exportRequestId, HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        ExportRequest exportRequest = getOwnedExportRequest(exportRequestId, coupleMember);
        if (exportRequest.getStatus() != ExportStatus.REQUESTED && exportRequest.getStatus() != ExportStatus.PREVIEWED) {
            throw new BusinessException(ErrorCode.EXPORT_STATE_INVALID);
        }

        exportRequest.cancel(appTimeProvider.nowUtcDateTime());
        return new CancelExportResponse(exportRequest.getId(), exportRequest.getStatus());
    }

    @Transactional
    public DeleteExportResponse deleteExport(Long exportRequestId, HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        ExportRequest exportRequest = getOwnedExportRequest(exportRequestId, coupleMember);
        if (exportRequest.getStatus() != ExportStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.EXPORT_STATE_INVALID);
        }

        exportItemRepository.deleteAllByExportRequest_Id(exportRequest.getId());
        exportRequestRepository.delete(exportRequest);
        return new DeleteExportResponse(exportRequest.getId(), true);
    }

    public DownloadPayload downloadExport(Long exportRequestId, String format, HttpSession session) {
        CoupleMember coupleMember = getAuthenticatedCoupleMember(session);
        ExportRequest exportRequest = getOwnedExportRequest(exportRequestId, coupleMember);
        if (exportRequest.getStatus() != ExportStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.EXPORT_NOT_COMPLETED);
        }

        String normalizedFormat = normalizeFormat(format);
        if ("json".equals(normalizedFormat)) {
            return new DownloadPayload(
                    "couple-diary-%d.json".formatted(exportRequest.getId()),
                    "application/json; charset=UTF-8",
                    exportRequest.getJsonPayload()
            );
        }
        if ("text".equals(normalizedFormat)) {
            return new DownloadPayload(
                    "couple-diary-%d.txt".formatted(exportRequest.getId()),
                    "text/plain; charset=UTF-8",
                    exportRequest.getTextPayload()
            );
        }
        throw new BusinessException(ErrorCode.EXPORT_FORMAT_INVALID);
    }

    private Set<Long> sanitizeDailyQuestionIds(List<Long> dailyQuestionIds) {
        if (dailyQuestionIds == null || dailyQuestionIds.isEmpty()) {
            throw new BusinessException(ErrorCode.EXPORT_ITEM_INVALID);
        }
        return new LinkedHashSet<>(dailyQuestionIds);
    }

    private List<DailyQuestion> loadValidExportDailyQuestions(CoupleMember coupleMember, Collection<Long> dailyQuestionIds) {
        List<DailyQuestion> dailyQuestions = dailyQuestionRepository.findAllByCouple_IdAndIdIn(
                coupleMember.getCouple().getId(),
                dailyQuestionIds
        );
        if (dailyQuestions.size() != dailyQuestionIds.size()) {
            throw new BusinessException(ErrorCode.EXPORT_ITEM_INVALID);
        }

        Map<Long, List<Answer>> answersByDailyQuestionId = answerRepository.findAllByDailyQuestion_IdIn(dailyQuestionIds).stream()
                .collect(Collectors.groupingBy(answer -> answer.getDailyQuestion().getId()));

        boolean containsLockedEntry = dailyQuestions.stream().anyMatch(dailyQuestion -> {
            List<Answer> answers = answersByDailyQuestionId.getOrDefault(dailyQuestion.getId(), List.of());
            return !isFullyAnswered(answers, coupleMember.getCouple().getId());
        });
        if (containsLockedEntry) {
            throw new BusinessException(ErrorCode.EXPORT_ITEM_INVALID);
        }

        return dailyQuestions.stream()
                .sorted(Comparator.comparing(DailyQuestion::getQuestionDate).thenComparing(DailyQuestion::getId))
                .toList();
    }

    private boolean isFullyAnswered(List<Answer> answers, Long coupleId) {
        List<CoupleMember> members = coupleMemberRepository.findAllByCouple_Id(coupleId);
        if (members.size() != 2) {
            return false;
        }
        Set<Long> memberIds = members.stream().map(member -> member.getUser().getId()).collect(Collectors.toSet());
        Set<Long> answeredUserIds = answers.stream().map(answer -> answer.getUser().getId()).collect(Collectors.toSet());
        return answeredUserIds.containsAll(memberIds);
    }

    private ExportRequest getOwnedExportRequest(Long exportRequestId, CoupleMember coupleMember) {
        return exportRequestRepository.findByIdAndCouple_Id(exportRequestId, coupleMember.getCouple().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private Map<Long, Integer> loadItemCounts(List<ExportRequest> exportRequests) {
        List<Long> exportRequestIds = exportRequests.stream()
                .map(ExportRequest::getId)
                .toList();
        if (exportRequestIds.isEmpty()) {
            return Map.of();
        }

        return exportItemRepository.countByExportRequestIds(exportRequestIds).stream()
                .collect(Collectors.toMap(
                        ExportItemRepository.ExportItemCount::getExportRequestId,
                        count -> count.getItemCount().intValue()
                ));
    }

    private ExportOrderSummaryResponse toOrderSummary(
            ExportRequest exportRequest,
            Map<Long, Integer> itemCountsByExportRequestId
    ) {
        return new ExportOrderSummaryResponse(
                exportRequest.getId(),
                exportRequest.getStatus(),
                itemCountsByExportRequestId.getOrDefault(exportRequest.getId(), 0),
                exportRequest.getCreatedAt(),
                exportRequest.getPreviewedAt(),
                exportRequest.getCompletedAt(),
                exportRequest.getCancelledAt()
        );
    }

    private List<ExportDownloadLinkResponse> buildDownloadLinks(ExportRequest exportRequest) {
        return List.of(
                new ExportDownloadLinkResponse("json", "/api/exports/%d/download?format=json".formatted(exportRequest.getId())),
                new ExportDownloadLinkResponse("text", "/api/exports/%d/download?format=text".formatted(exportRequest.getId()))
        );
    }

    private List<ExportPreviewEntryResponse> readSnapshotEntries(ExportRequest exportRequest) {
        try {
            ExportSnapshotPayload payload = objectMapper.readValue(exportRequest.getJsonPayload(), ExportSnapshotPayload.class);
            return payload.entries();
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private List<ExportPreviewEntryResponse> buildPreviewEntries(ExportRequest exportRequest, Long coupleId) {
        List<ExportItem> exportItems = exportItemRepository.findAllByExportRequest_IdOrderBySortOrderAsc(exportRequest.getId());
        List<DailyQuestion> dailyQuestions = exportItems.stream()
                .map(ExportItem::getDailyQuestion)
                .sorted(Comparator.comparing(DailyQuestion::getQuestionDate).thenComparing(DailyQuestion::getId))
                .toList();
        List<Long> dailyQuestionIds = dailyQuestions.stream().map(DailyQuestion::getId).toList();

        Map<Long, List<Answer>> answersByDailyQuestionId = answerRepository.findAllByDailyQuestion_IdIn(dailyQuestionIds).stream()
                .collect(Collectors.groupingBy(answer -> answer.getDailyQuestion().getId()));
        List<CoupleMember> coupleMembers = coupleMemberRepository.findAllByCouple_IdOrderByJoinedAtAscIdAsc(coupleId);
        Map<Long, User> usersById = coupleMembers.stream()
                .map(CoupleMember::getUser)
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return dailyQuestions.stream()
                .map(dailyQuestion -> toPreviewEntry(dailyQuestion, answersByDailyQuestionId.getOrDefault(dailyQuestion.getId(), List.of()), usersById))
                .toList();
    }

    private ExportPreviewEntryResponse toPreviewEntry(
            DailyQuestion dailyQuestion,
            List<Answer> answers,
            Map<Long, User> usersById
    ) {
        Map<Long, Answer> answersByUserId = answers.stream()
                .collect(Collectors.toMap(answer -> answer.getUser().getId(), Function.identity()));
        List<ExportPreviewAnswerResponse> answerResponses = usersById.values().stream()
                .map(user -> answersByUserId.get(user.getId()))
                .filter(answer -> answer != null)
                .map(answer -> new ExportPreviewAnswerResponse(answer.getUser().getDisplayName(), answer.getContent()))
                .toList();

        return new ExportPreviewEntryResponse(
                dailyQuestion.getQuestionDate(),
                dailyQuestion.getQuestion().getContent(),
                answerResponses
        );
    }

    private String buildJsonPayload(
            ExportRequest exportRequest,
            List<ExportPreviewEntryResponse> entries,
            LocalDateTime completedAt
    ) {
        try {
            return objectMapper.writeValueAsString(new ExportSnapshotPayload(
                    exportRequest.getId(),
                    exportRequest.getCouple().getId(),
                    completedAt,
                    entries
            ));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String buildTextPayload(List<ExportPreviewEntryResponse> entries) {
        StringBuilder builder = new StringBuilder();
        for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
            ExportPreviewEntryResponse entry = entries.get(entryIndex);
            builder.append(entry.date()).append('\n');
            builder.append("Q. ").append(entry.question()).append("\n\n");

            for (int answerIndex = 0; answerIndex < entry.answers().size(); answerIndex++) {
                ExportPreviewAnswerResponse answer = entry.answers().get(answerIndex);
                builder.append(answer.displayName()).append('\n');
                builder.append(answer.content()).append('\n');
                if (answerIndex < entry.answers().size() - 1) {
                    builder.append('\n');
                }
            }

            if (entryIndex < entries.size() - 1) {
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }

    private String normalizeFormat(String format) {
        return Optional.ofNullable(format)
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse("");
    }

    private CoupleMember getAuthenticatedCoupleMember(HttpSession session) {
        Long userId = (Long) session.getAttribute(AuthService.SESSION_USER_ID);
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));

        return coupleMemberRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    public record DownloadPayload(
            String fileName,
            String contentType,
            String body
    ) {
    }

    private record ExportSnapshotPayload(
            Long exportRequestId,
            Long coupleId,
            LocalDateTime exportedAt,
            List<ExportPreviewEntryResponse> entries
    ) {
    }
}
