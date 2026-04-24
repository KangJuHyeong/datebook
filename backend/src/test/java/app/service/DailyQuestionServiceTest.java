package app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
import app.common.time.AppTimeProvider;
import app.domain.Answer;
import app.domain.AnswerState;
import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.DailyQuestion;
import app.domain.PartnerAnswerStatus;
import app.domain.Question;
import app.domain.User;
import app.dto.question.TodayQuestionResponse;
import app.repository.AnswerRepository;
import app.repository.CoupleMemberRepository;
import app.repository.DailyQuestionRepository;
import app.repository.QuestionRepository;
import app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DailyQuestionServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T02:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private CoupleMemberRepository coupleMemberRepository;

    @Mock
    private DailyQuestionRepository dailyQuestionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private HttpSession session;

    @Spy
    private AppTimeProvider appTimeProvider = new AppTimeProvider(
            Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
            Clock.fixed(FIXED_INSTANT, ZoneId.of("Asia/Seoul"))
    );

    @InjectMocks
    private DailyQuestionService dailyQuestionService;

    @Test
    @DisplayName("같은 커플 같은 서울 날짜에는 같은 오늘 질문을 반환한다")
    void returnsExistingDailyQuestionForSameCoupleAndDate() {
        Fixture fixture = fixture();
        DailyQuestion dailyQuestion = dailyQuestion(fixture.couple, question("오늘 질문", 1), LocalDate.of(2026, 4, 24), 100L);
        stubAuthenticatedMember(fixture);
        when(dailyQuestionRepository.findByCouple_IdAndQuestionDate(fixture.couple.getId(), LocalDate.of(2026, 4, 24)))
                .thenReturn(Optional.of(dailyQuestion));
        when(answerRepository.findAllByDailyQuestion_Id(100L)).thenReturn(List.of());

        TodayQuestionResponse first = dailyQuestionService.getTodayQuestion(session);
        TodayQuestionResponse second = dailyQuestionService.getTodayQuestion(session);

        assertThat(first.dailyQuestionId()).isEqualTo(100L);
        assertThat(second.dailyQuestionId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("기존 질문 개수 modulo 규칙으로 active 질문을 순환 배정한다")
    void assignsQuestionByRotationOrder() {
        Fixture fixture = fixture();
        Question firstQuestion = question("질문 1", 1);
        Question secondQuestion = question("질문 2", 2);
        Question thirdQuestion = question("질문 3", 3);
        stubAuthenticatedMember(fixture);
        when(dailyQuestionRepository.findByCouple_IdAndQuestionDate(fixture.couple.getId(), LocalDate.of(2026, 4, 24)))
                .thenReturn(Optional.empty());
        when(questionRepository.findByActiveTrueOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(firstQuestion, secondQuestion, thirdQuestion));
        when(dailyQuestionRepository.countByCouple_Id(fixture.couple.getId())).thenReturn(4L);
        when(dailyQuestionRepository.saveAndFlush(any(DailyQuestion.class))).thenAnswer(invocation -> {
            DailyQuestion saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L);
            return saved;
        });
        when(answerRepository.findAllByDailyQuestion_Id(101L)).thenReturn(List.of());

        TodayQuestionResponse response = dailyQuestionService.getTodayQuestion(session);

        assertThat(response.question()).isEqualTo("질문 2");
        assertThat(response.answerState()).isEqualTo(AnswerState.NOT_ANSWERED);
    }

    @Test
    @DisplayName("active 질문이 없으면 CONFIGURATION_ERROR를 던진다")
    void throwsConfigurationErrorWhenNoActiveQuestions() {
        Fixture fixture = fixture();
        stubAuthenticatedMember(fixture);
        when(dailyQuestionRepository.findByCouple_IdAndQuestionDate(fixture.couple.getId(), LocalDate.of(2026, 4, 24)))
                .thenReturn(Optional.empty());
        when(questionRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of());

        assertThatThrownBy(() -> dailyQuestionService.getTodayQuestion(session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFIGURATION_ERROR);
    }

    @Test
    @DisplayName("오늘 질문 응답의 답변 상태와 공개 범위를 계산한다")
    void resolvesAnswerStatesAndPartnerVisibility() {
        Fixture fixture = fixture();
        User partner = new User("partner@example.com", "hash", "도윤");
        ReflectionTestUtils.setField(partner, "id", 2L);
        DailyQuestion dailyQuestion = dailyQuestion(fixture.couple, question("오늘 질문", 1), LocalDate.of(2026, 4, 24), 100L);
        stubAuthenticatedMember(fixture);
        when(dailyQuestionRepository.findByCouple_IdAndQuestionDate(fixture.couple.getId(), LocalDate.of(2026, 4, 24)))
                .thenReturn(Optional.of(dailyQuestion));

        when(answerRepository.findAllByDailyQuestion_Id(100L)).thenReturn(List.of());
        TodayQuestionResponse notAnswered = dailyQuestionService.getTodayQuestion(session);

        Answer myAnswer = answer(dailyQuestion, fixture.user, "내 답변", 201L);
        when(answerRepository.findAllByDailyQuestion_Id(100L)).thenReturn(List.of(myAnswer));
        TodayQuestionResponse myAnswered = dailyQuestionService.getTodayQuestion(session);

        Answer partnerAnswer = answer(dailyQuestion, partner, "상대 답변", 202L);
        when(answerRepository.findAllByDailyQuestion_Id(100L)).thenReturn(List.of(partnerAnswer));
        TodayQuestionResponse partnerAnswered = dailyQuestionService.getTodayQuestion(session);

        when(answerRepository.findAllByDailyQuestion_Id(100L)).thenReturn(List.of(myAnswer, partnerAnswer));
        TodayQuestionResponse bothAnswered = dailyQuestionService.getTodayQuestion(session);

        assertThat(notAnswered.answerState()).isEqualTo(AnswerState.NOT_ANSWERED);
        assertThat(notAnswered.partnerAnswer().status()).isEqualTo(PartnerAnswerStatus.LOCKED);
        assertThat(notAnswered.partnerAnswer().content()).isNull();

        assertThat(myAnswered.answerState()).isEqualTo(AnswerState.MY_ANSWERED_PARTNER_WAITING);
        assertThat(myAnswered.myAnswer()).isNotNull();
        assertThat(myAnswered.partnerAnswer().status()).isEqualTo(PartnerAnswerStatus.LOCKED);

        assertThat(partnerAnswered.answerState()).isEqualTo(AnswerState.PARTNER_ANSWERED_ME_WAITING);
        assertThat(partnerAnswered.partnerAnswer().status()).isEqualTo(PartnerAnswerStatus.ANSWERED_LOCKED);
        assertThat(partnerAnswered.partnerAnswer().content()).isNull();

        assertThat(bothAnswered.answerState()).isEqualTo(AnswerState.BOTH_ANSWERED);
        assertThat(bothAnswered.partnerAnswer().status()).isEqualTo(PartnerAnswerStatus.REVEALED);
        assertThat(bothAnswered.partnerAnswer().content()).isEqualTo("상대 답변");
        assertThat(bothAnswered.isFullyAnswered()).isTrue();
    }

    private void stubAuthenticatedMember(Fixture fixture) {
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
    }

    private Fixture fixture() {
        User user = new User("user@example.com", "hash", "민지");
        Couple couple = new Couple();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(couple, "id", 10L);
        return new Fixture(user, couple, new CoupleMember(couple, user, appTimeProvider.nowUtcDateTime()));
    }

    private Question question(String content, int sortOrder) {
        Question question = new Question(content, true, sortOrder);
        ReflectionTestUtils.setField(question, "id", (long) sortOrder);
        return question;
    }

    private DailyQuestion dailyQuestion(Couple couple, Question question, LocalDate date, Long id) {
        DailyQuestion dailyQuestion = new DailyQuestion(couple, question, date);
        ReflectionTestUtils.setField(dailyQuestion, "id", id);
        return dailyQuestion;
    }

    private Answer answer(DailyQuestion dailyQuestion, User user, String content, Long id) {
        Answer answer = new Answer(dailyQuestion, user, content);
        ReflectionTestUtils.setField(answer, "id", id);
        return answer;
    }

    private record Fixture(User user, Couple couple, CoupleMember member) {
    }
}
