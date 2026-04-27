package app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import app.domain.Answer;
import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.DailyQuestion;
import app.domain.Question;
import app.domain.User;
import app.dto.diary.DiaryResponse;
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
class DiaryServiceTest {

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
    private DiaryService diaryService;

    @Test
    @DisplayName("기록 목록은 날짜 내림차순 상태와 export 가능 여부를 계산한다")
    void getDiaryReturnsEntriesInDescendingDateOrder() {
        User me = user(1L, "me@example.com", "민지");
        User partner = user(2L, "partner@example.com", "도윤");
        Couple couple = couple(10L);
        CoupleMember member = new CoupleMember(couple, me, LocalDateTime.of(2026, 4, 24, 0, 0));
        DailyQuestion newer = dailyQuestion(101L, couple, "질문 2", LocalDate.of(2026, 4, 24));
        DailyQuestion older = dailyQuestion(100L, couple, "질문 1", LocalDate.of(2026, 4, 23));

        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(me.getId());
        when(userRepository.findById(me.getId())).thenReturn(Optional.of(me));
        when(coupleMemberRepository.findByUser_Id(me.getId())).thenReturn(Optional.of(member));
        when(dailyQuestionRepository.findAllByCouple_IdOrderByQuestionDateDesc(couple.getId()))
                .thenReturn(List.of(newer, older));
        when(answerRepository.findAllByDailyQuestion_IdIn(List.of(newer.getId(), older.getId())))
                .thenReturn(List.of(
                        new Answer(newer, partner, "상대만 답변"),
                        new Answer(older, me, "내 답변"),
                        new Answer(older, partner, "상대 답변")
                ));

        DiaryResponse response = diaryService.getDiary(session);

        assertThat(response.entries()).hasSize(2);
        assertThat(response.entries().get(0).dailyQuestionId()).isEqualTo(newer.getId());
        assertThat(response.entries().get(0).myAnswerStatus()).isEqualTo("NOT_ANSWERED");
        assertThat(response.entries().get(0).partnerAnswerStatus()).isEqualTo("ANSWERED_LOCKED");
        assertThat(response.entries().get(0).myAnswer()).isNull();
        assertThat(response.entries().get(0).partnerAnswer()).isNull();
        assertThat(response.entries().get(0).exportable()).isFalse();
        assertThat(response.entries().get(1).dailyQuestionId()).isEqualTo(older.getId());
        assertThat(response.entries().get(1).myAnswerStatus()).isEqualTo("ANSWERED");
        assertThat(response.entries().get(1).partnerAnswerStatus()).isEqualTo("REVEALED");
        assertThat(response.entries().get(1).myAnswer().content()).isEqualTo("내 답변");
        assertThat(response.entries().get(1).partnerAnswer().displayName()).isEqualTo("도윤");
        assertThat(response.entries().get(1).partnerAnswer().content()).isEqualTo("상대 답변");
        assertThat(response.entries().get(1).exportable()).isTrue();
    }

    private User user(Long id, String email, String displayName) {
        User user = new User(email, "hash", displayName);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Couple couple(Long id) {
        Couple couple = new Couple();
        ReflectionTestUtils.setField(couple, "id", id);
        return couple;
    }

    private DailyQuestion dailyQuestion(Long id, Couple couple, String questionContent, LocalDate date) {
        Question question = new Question(questionContent, true, 1);
        ReflectionTestUtils.setField(question, "id", id + 1000);
        DailyQuestion dailyQuestion = new DailyQuestion(couple, question, date);
        ReflectionTestUtils.setField(dailyQuestion, "id", id);
        return dailyQuestion;
    }
}
