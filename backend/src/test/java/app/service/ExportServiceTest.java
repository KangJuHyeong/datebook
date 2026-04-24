package app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyCollection;
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
import app.domain.ExportRequest;
import app.domain.ExportStatus;
import app.domain.Question;
import app.domain.User;
import app.dto.export.CreateExportRequest;
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
    @DisplayName("완료 전 다운로드는 거부한다")
    void downloadRejectsNotCompletedExport() {
        Fixture fixture = fixture();
        ExportRequest exportRequest = new ExportRequest(fixture.couple, fixture.user, ExportStatus.PREVIEWED);
        ReflectionTestUtils.setField(exportRequest, "id", 300L);

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(exportRequestRepository.findById(300L)).thenReturn(Optional.of(exportRequest));

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
        when(exportRequestRepository.findById(300L)).thenReturn(Optional.of(exportRequest));

        ExportService.DownloadPayload payload = exportService.downloadExport(300L, "text", session);

        assertThat(payload.fileName()).isEqualTo("couple-diary-300.txt");
        assertThat(payload.body()).isEqualTo("snapshot text");
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

    private record Fixture(User user, Couple couple, CoupleMember member) {
    }
}
