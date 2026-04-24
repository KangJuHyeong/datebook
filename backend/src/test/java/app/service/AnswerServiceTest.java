package app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
import app.domain.Answer;
import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.DailyQuestion;
import app.domain.Question;
import app.domain.User;
import app.dto.answer.AnswerResponse;
import app.dto.answer.UpdateAnswerRequest;
import app.repository.AnswerRepository;
import app.repository.CoupleMemberRepository;
import app.repository.DailyQuestionRepository;
import app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CoupleMemberRepository coupleMemberRepository;

    @Mock
    private DailyQuestionRepository dailyQuestionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AnswerService answerService;

    @Test
    @DisplayName("본인 답변은 수정할 수 있다")
    void updateAnswerSucceedsForOwner() {
        Fixture fixture = fixture();
        Answer answer = answer(fixture.dailyQuestion, fixture.user, "기존 답변", 200L);
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(answerRepository.findById(200L)).thenReturn(Optional.of(answer));
        when(answerRepository.saveAndFlush(answer)).thenReturn(answer);

        AnswerResponse response = answerService.updateAnswer(200L, new UpdateAnswerRequest("수정한 답변"), session);

        assertThat(response.content()).isEqualTo("수정한 답변");
    }

    @Test
    @DisplayName("다른 사용자의 답변 수정은 ANSWER_NOT_OWNED로 거부한다")
    void updateAnswerRejectsOtherUsersAnswer() {
        Fixture fixture = fixture();
        User partner = new User("partner@example.com", "hash", "도윤");
        ReflectionTestUtils.setField(partner, "id", 2L);
        Answer answer = answer(fixture.dailyQuestion, partner, "상대 답변", 201L);
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(fixture.user.getId());
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(coupleMemberRepository.findByUser_Id(fixture.user.getId())).thenReturn(Optional.of(fixture.member));
        when(answerRepository.findById(201L)).thenReturn(Optional.of(answer));

        assertThatThrownBy(() -> answerService.updateAnswer(201L, new UpdateAnswerRequest("수정 시도"), session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ANSWER_NOT_OWNED);
    }

    private Fixture fixture() {
        User user = new User("user@example.com", "hash", "민지");
        Couple couple = new Couple();
        Question question = new Question("오늘 질문", true, 1);
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(couple, "id", 10L);
        ReflectionTestUtils.setField(question, "id", 100L);
        DailyQuestion dailyQuestion = new DailyQuestion(couple, question, LocalDate.of(2026, 4, 24));
        ReflectionTestUtils.setField(dailyQuestion, "id", 101L);
        return new Fixture(user, new CoupleMember(couple, user, LocalDateTime.of(2026, 4, 24, 0, 0)), dailyQuestion);
    }

    private Answer answer(DailyQuestion dailyQuestion, User user, String content, Long id) {
        Answer answer = new Answer(dailyQuestion, user, content);
        ReflectionTestUtils.setField(answer, "id", id);
        return answer;
    }

    private record Fixture(User user, CoupleMember member, DailyQuestion dailyQuestion) {
    }
}
