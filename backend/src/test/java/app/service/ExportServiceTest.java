package app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
import app.common.time.AppTimeProvider;
import app.domain.Answer;
import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.DailyQuestion;
import app.domain.ExportItem;
import app.domain.ExportRequest;
import app.domain.ExportStatus;
import app.domain.Question;
import app.domain.User;
import app.dto.export.CreateExportRequest;
import app.dto.export.ExportOrderDetailResponse;
import app.dto.export.ExportOrderListResponse;
import app.repository.AnswerRepository;
import app.repository.CoupleMemberRepository;
import app.repository.DailyQuestionRepository;
import app.repository.ExportItemRepository;
import app.repository.ExportRequestRepository;
import app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T02:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private CoupleMemberRepository coupleMemberRepository;

    @Mock
    private DailyQuestionRepository dailyQuestionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private ExportRequestRepository exportRequestRepository;

    @Mock
    private ExportItemRepository exportItemRepository;

    @Mock
    private HttpSession session;

    @Spy
    private AppTimeProvider appTimeProvider = new AppTimeProvider(
            Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
            Clock.fixed(FIXED_INSTANT, ZoneId.of("Asia/Seoul"))
    );

    private ExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExportService(
                userRepository,
                coupleMemberRepository,
                dailyQuestionRepository,
                answerRepository,
                exportRequestRepository,
                exportItemRepository,
                appTimeProvider,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    @DisplayName("진행 중과 완료된 커플 주문 목록을 생성일 내림차순으로 반환한다")
    void listExportsReturnsPreviewedAndCompletedOrders() {
        Fixture fixture = fixture();
        ExportRequest completed = exportRequest(301L, fixture, ExportStatus.COMPLETED, LocalDateTime.of(2026, 4, 24, 2, 0));
        completed.markCompleted(appTimeProvider.nowUtcDateTime(), "{\"exportRequestId\":301}", "snapshot");
        ExportRequest previewed = exportRequest(302L, fixture, ExportStatus.PREVIEWED, LocalDateTime.of(2026, 4, 24, 3, 0));

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findAllByCouple_IdAndStatusInOrderByCreatedAtDesc(
                eq(fixture.couple.getId()),
                eq(List.of(ExportStatus.PREVIEWED, ExportStatus.COMPLETED))
        )).thenReturn(List.of(previewed, completed));
        when(exportItemRepository.countByExportRequestIds(List.of(302L, 301L)))
                .thenReturn(List.of(new ItemCount(302L, 2), new ItemCount(301L, 1)));

        ExportOrderListResponse response = exportService.listExports(session);

        assertThat(response.orders()).extracting("exportRequestId").containsExactly(302L, 301L);
        assertThat(response.orders()).extracting("status").containsExactly(ExportStatus.PREVIEWED, ExportStatus.COMPLETED);
        assertThat(response.orders()).extracting("itemCount").containsExactly(2, 1);
        verify(exportItemRepository, never()).countByExportRequest_Id(302L);
        verify(exportItemRepository, never()).countByExportRequest_Id(301L);
    }

    @Test
    @DisplayName("미리보기 주문 상세는 entries를 반환해 주문 완료를 이어갈 수 있다")
    void getExportReturnsPreviewEntriesForPreviewedOrder() {
        Fixture fixture = fixture();
        ExportRequest exportRequest = exportRequest(300L, fixture, ExportStatus.REQUESTED, LocalDateTime.of(2026, 4, 24, 3, 0));
        exportRequest.markPreviewed(appTimeProvider.nowUtcDateTime(), snapshotJson(300L, "미리보기 질문", "내 답변", "상대 답변"));

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));
        when(exportItemRepository.countByExportRequest_Id(300L)).thenReturn(1);

        ExportOrderDetailResponse response = exportService.getExport(300L, session);

        assertThat(response.status()).isEqualTo(ExportStatus.PREVIEWED);
        assertThat(response.entries()).hasSize(1);
        assertThat(response.entries().get(0).question()).isEqualTo("미리보기 질문");
        assertThat(response.entries().get(0).answers()).extracting("content").containsExactly("내 답변", "상대 답변");
        assertThat(response.downloads()).isEmpty();
        verify(answerRepository, never()).findAllByDailyQuestion_IdIn(List.of(101L));
    }

    @Test
    @DisplayName("완료된 주문 상세는 저장 스냅샷 entries와 다운로드 링크를 반환한다")
    void getExportReturnsSnapshotEntriesAndDownloadLinksForCompletedOrder() {
        Fixture fixture = fixture();
        ExportRequest exportRequest = exportRequest(300L, fixture, ExportStatus.REQUESTED, LocalDateTime.of(2026, 4, 24, 3, 0));
        exportRequest.markCompleted(appTimeProvider.nowUtcDateTime(), """
                {
                  "exportRequestId": 300,
                  "coupleId": 10,
                  "exportedAt": "2026-04-24T02:00:00",
                  "entries": [
                    {
                      "date": "2026-04-24",
                      "question": "저장된 질문",
                      "answers": [
                        {
                          "displayName": "민지",
                          "content": "저장된 답변"
                        }
                      ]
                    }
                  ]
                }
                """, "snapshot");

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));
        when(exportItemRepository.countByExportRequest_Id(300L)).thenReturn(1);

        ExportOrderDetailResponse response = exportService.getExport(300L, session);

        assertThat(response.status()).isEqualTo(ExportStatus.COMPLETED);
        assertThat(response.entries()).hasSize(1);
        assertThat(response.entries().get(0).question()).isEqualTo("저장된 질문");
        assertThat(response.entries().get(0).answers()).extracting("content").containsExactly("저장된 답변");
        assertThat(response.downloads()).extracting("format").containsExactly("json", "text");
    }

    @Test
    @DisplayName("완료된 주문 상세는 원본 답변이 바뀌어도 저장 스냅샷을 반환한다")
    void getExportCompletedOrderUsesSnapshotInsteadOfCurrentAnswers() {
        Fixture fixture = fixture();
        DailyQuestion dailyQuestion = dailyQuestion(101L, fixture.couple, "수정된 원본 질문", LocalDate.of(2026, 4, 24));
        ExportRequest exportRequest = exportRequest(300L, fixture, ExportStatus.REQUESTED, LocalDateTime.of(2026, 4, 24, 3, 0));
        exportRequest.markCompleted(appTimeProvider.nowUtcDateTime(), """
                {
                  "exportRequestId": 300,
                  "coupleId": 10,
                  "exportedAt": "2026-04-24T02:00:00",
                  "entries": [
                    {
                      "date": "2026-04-24",
                      "question": "완료 시점 질문",
                      "answers": [
                        {
                          "displayName": "민지",
                          "content": "완료 시점 답변"
                        }
                      ]
                    }
                  ]
                }
                """, "snapshot");

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));
        when(exportItemRepository.countByExportRequest_Id(300L)).thenReturn(1);

        ExportOrderDetailResponse response = exportService.getExport(300L, session);

        assertThat(response.entries()).hasSize(1);
        assertThat(response.entries().get(0).question()).isEqualTo("완료 시점 질문");
        assertThat(response.entries().get(0).answers()).extracting("content").containsExactly("완료 시점 답변");
        verify(answerRepository, never()).findAllByDailyQuestion_IdIn(List.of(dailyQuestion.getId()));
    }

    @Test
    @DisplayName("다른 커플 주문 상세 접근은 거부한다")
    void getExportRejectsOtherCoupleOrder() {
        Fixture fixture = fixture();
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportService.getExport(300L, session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("빈 주문 항목은 거부한다")
    void createExportRejectsEmptyItems() {
        Fixture fixture = fixture();
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));

        assertThatThrownBy(() -> exportService.createExport(new CreateExportRequest(List.of()), session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPORT_ITEM_INVALID);
    }

    @Test
    @DisplayName("둘 다 답하지 않은 기록은 주문할 수 없다")
    void createExportRejectsLockedItem() {
        Fixture fixture = fixture();
        DailyQuestion dailyQuestion = dailyQuestion(101L, fixture.couple, "질문", LocalDate.of(2026, 4, 24));
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(dailyQuestionRepository.findAllByCouple_IdAndIdIn(eq(fixture.couple.getId()), anyCollection()))
                .thenReturn(List.of(dailyQuestion));
        when(answerRepository.findAllByDailyQuestion_IdIn(anyCollection()))
                .thenReturn(List.of(new Answer(dailyQuestion, fixture.user, "내 답변")));
        when(coupleMemberRepository.findAllByCouple_Id(fixture.couple.getId()))
                .thenReturn(List.of(
                        fixture.member,
                        new CoupleMember(fixture.couple, user(2L, "partner@example.com", "도윤"), LocalDateTime.of(2026, 4, 24, 0, 1))
                ));

        assertThatThrownBy(() -> exportService.createExport(new CreateExportRequest(List.of(101L)), session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPORT_ITEM_INVALID);
    }

    @Test
    @DisplayName("미리보기는 사용자에게 보여준 entries를 스냅샷으로 저장한다")
    void previewExportStoresSnapshotEntries() {
        Fixture fixture = fixture();
        User partner = user(2L, "partner@example.com", "도윤");
        CoupleMember partnerMember = new CoupleMember(fixture.couple, partner, LocalDateTime.of(2026, 4, 24, 0, 1));
        DailyQuestion dailyQuestion = dailyQuestion(101L, fixture.couple, "미리보기 질문", LocalDate.of(2026, 4, 24));
        ExportRequest exportRequest = exportRequest(300L, fixture, ExportStatus.REQUESTED, LocalDateTime.of(2026, 4, 24, 3, 0));
        ExportItem exportItem = new ExportItem(exportRequest, dailyQuestion, 0);

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));
        when(exportItemRepository.findAllByExportRequest_IdOrderBySortOrderAsc(300L)).thenReturn(List.of(exportItem));
        when(answerRepository.findAllByDailyQuestion_IdIn(List.of(101L))).thenReturn(List.of(
                new Answer(dailyQuestion, fixture.user, "미리보기 내 답변"),
                new Answer(dailyQuestion, partner, "미리보기 상대 답변")
        ));
        when(coupleMemberRepository.findAllByCouple_IdOrderByJoinedAtAscIdAsc(fixture.couple.getId()))
                .thenReturn(List.of(fixture.member, partnerMember));

        var response = exportService.previewExport(300L, session);

        assertThat(response.status()).isEqualTo(ExportStatus.PREVIEWED);
        assertThat(exportRequest.getJsonPayload()).contains("미리보기 질문", "미리보기 내 답변", "미리보기 상대 답변");
    }

    @Test
    @DisplayName("주문 완료는 미리보기 스냅샷을 확정하고 원본 답변을 다시 조회하지 않는다")
    void completeExportUsesPreviewSnapshotInsteadOfCurrentAnswers() {
        Fixture fixture = fixture();
        ExportRequest exportRequest = exportRequest(300L, fixture, ExportStatus.REQUESTED, LocalDateTime.of(2026, 4, 24, 3, 0));
        exportRequest.markPreviewed(appTimeProvider.nowUtcDateTime(), snapshotJson(300L, "미리보기 시점 질문", "미리보기 시점 답변", "상대 답변"));

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));

        var response = exportService.completeExport(300L, session);

        assertThat(response.status()).isEqualTo(ExportStatus.COMPLETED);
        assertThat(exportRequest.getJsonPayload()).contains("미리보기 시점 질문", "미리보기 시점 답변");
        assertThat(exportRequest.getTextPayload()).contains("미리보기 시점 질문", "미리보기 시점 답변");
        verify(answerRepository, never()).findAllByDailyQuestion_IdIn(anyCollection());
    }

    @Test
    @DisplayName("미리보기 주문 취소는 저장된 답변 스냅샷을 제거한다")
    void cancelPreviewedExportClearsPreviewSnapshot() {
        Fixture fixture = fixture();
        ExportRequest exportRequest = exportRequest(300L, fixture, ExportStatus.REQUESTED, LocalDateTime.of(2026, 4, 24, 3, 0));
        exportRequest.markPreviewed(appTimeProvider.nowUtcDateTime(), snapshotJson(300L, "질문", "내 답변", "상대 답변"));

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));

        var response = exportService.cancelExport(300L, session);

        assertThat(response.status()).isEqualTo(ExportStatus.CANCELLED);
        assertThat(exportRequest.getJsonPayload()).isNull();
        assertThat(exportRequest.getTextPayload()).isNull();
    }

    @Test
    @DisplayName("완료 전 다운로드는 거부한다")
    void downloadRejectsNotCompletedExport() {
        Fixture fixture = fixture();
        ExportRequest exportRequest = new ExportRequest(fixture.couple, fixture.user, ExportStatus.PREVIEWED);
        ReflectionTestUtils.setField(exportRequest, "id", 300L);

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));

        assertThatThrownBy(() -> exportService.downloadExport(300L, "json", session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPORT_NOT_COMPLETED);
    }

    @Test
    @DisplayName("완료된 주문 다운로드는 저장된 스냅샷을 반환한다")
    void downloadReturnsStoredSnapshot() {
        Fixture fixture = fixture();
        ExportRequest exportRequest = new ExportRequest(fixture.couple, fixture.user, ExportStatus.REQUESTED);
        ReflectionTestUtils.setField(exportRequest, "id", 300L);
        exportRequest.markCompleted(
                appTimeProvider.nowUtcDateTime(),
                "{\"exportRequestId\":300}",
                "snapshot text"
        );

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));

        ExportService.DownloadPayload payload = exportService.downloadExport(300L, "text", session);

        assertThat(payload.fileName()).isEqualTo("couple-diary-300.txt");
        assertThat(payload.body()).isEqualTo("snapshot text");
    }

    @Test
    @DisplayName("완료된 주문 삭제는 export item과 request를 제거한다")
    void deleteExportDeletesCompletedOrder() {
        Fixture fixture = fixture();
        ExportRequest exportRequest = exportRequest(300L, fixture, ExportStatus.REQUESTED, LocalDateTime.of(2026, 4, 24, 3, 0));
        exportRequest.markCompleted(appTimeProvider.nowUtcDateTime(), "{\"exportRequestId\":300,\"entries\":[]}", "snapshot");

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));

        var response = exportService.deleteExport(300L, session);

        assertThat(response.exportRequestId()).isEqualTo(300L);
        assertThat(response.deleted()).isTrue();
        verify(exportItemRepository).deleteAllByExportRequest_Id(300L);
        verify(exportRequestRepository).delete(exportRequest);
    }

    @Test
    @DisplayName("다른 커플 완료 주문 삭제는 거부한다")
    void deleteExportRejectsOtherCoupleOrder() {
        Fixture fixture = fixture();
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportService.deleteExport(300L, session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);

        verify(exportItemRepository, never()).deleteAllByExportRequest_Id(300L);
        verify(exportRequestRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("미리보기 주문 삭제는 거부하고 cancel 흐름을 사용해야 한다")
    void deleteExportRejectsPreviewedOrder() {
        Fixture fixture = fixture();
        ExportRequest exportRequest = exportRequest(300L, fixture, ExportStatus.PREVIEWED, LocalDateTime.of(2026, 4, 24, 3, 0));

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findByIdAndCouple_Id(300L, fixture.couple.getId())).thenReturn(Optional.of(exportRequest));

        assertThatThrownBy(() -> exportService.deleteExport(300L, session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPORT_STATE_INVALID);

        verify(exportItemRepository, never()).deleteAllByExportRequest_Id(300L);
        verify(exportRequestRepository, never()).delete(exportRequest);
    }

    private Fixture fixture() {
        User user = user(1L, "user@example.com", "민지");
        Couple couple = new Couple();
        ReflectionTestUtils.setField(couple, "id", 10L);
        return new Fixture(user, couple, new CoupleMember(couple, user, LocalDateTime.of(2026, 4, 24, 0, 0)));
    }

    private User user(Long id, String email, String displayName) {
        User user = new User(email, "hash", displayName);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private DailyQuestion dailyQuestion(Long id, Couple couple, String questionContent, LocalDate date) {
        Question question = new Question(questionContent, true, 1);
        ReflectionTestUtils.setField(question, "id", id + 1000);
        DailyQuestion dailyQuestion = new DailyQuestion(couple, question, date);
        ReflectionTestUtils.setField(dailyQuestion, "id", id);
        return dailyQuestion;
    }

    private ExportRequest exportRequest(Long id, Fixture fixture, ExportStatus status, LocalDateTime createdAt) {
        ExportRequest exportRequest = new ExportRequest(fixture.couple, fixture.user, status);
        ReflectionTestUtils.setField(exportRequest, "id", id);
        ReflectionTestUtils.setField(exportRequest, "createdAt", createdAt);
        return exportRequest;
    }

    private String snapshotJson(Long exportRequestId, String question, String myAnswer, String partnerAnswer) {
        return """
                {
                  "exportRequestId": %d,
                  "coupleId": 10,
                  "exportedAt": "2026-04-24T02:00:00",
                  "entries": [
                    {
                      "date": "2026-04-24",
                      "question": "%s",
                      "answers": [
                        {
                          "displayName": "민지",
                          "content": "%s"
                        },
                        {
                          "displayName": "도윤",
                          "content": "%s"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(exportRequestId, question, myAnswer, partnerAnswer);
    }

    private record ItemCount(Long exportRequestId, Long itemCount) implements ExportItemRepository.ExportItemCount {
        private ItemCount(Long exportRequestId, int itemCount) {
            this(exportRequestId, Long.valueOf(itemCount));
        }

        @Override
        public Long getExportRequestId() {
            return exportRequestId;
        }

        @Override
        public Long getItemCount() {
            return itemCount;
        }
    }

    private record Fixture(User user, Couple couple, CoupleMember member) {
    }
}
