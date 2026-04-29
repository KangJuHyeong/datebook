package app.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import app.domain.Answer;
import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.DailyQuestion;
import app.domain.Question;
import app.domain.User;
import app.repository.AnswerRepository;
import app.repository.CoupleMemberRepository;
import app.repository.CoupleRepository;
import app.repository.DailyQuestionRepository;
import app.repository.QuestionRepository;
import app.repository.UserRepository;
import app.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnswerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private DailyQuestionRepository dailyQuestionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/answers 는 같은 질문에 기존 답변이 있으면 수정으로 처리한다")
    void createAnswerUpsertsExistingAnswer() throws Exception {
        User user = createUser("answer@example.com", "민지");
        Couple couple = createCoupleWithMember(user);
        Question question = questionRepository.saveAndFlush(new Question("오늘 질문", true, 1));
        DailyQuestion dailyQuestion = dailyQuestionRepository.saveAndFlush(
                new DailyQuestion(couple, question, LocalDate.of(2026, 4, 24))
        );
        answerRepository.saveAndFlush(new Answer(dailyQuestion, user, "기존 답변"));

        mockMvc.perform(post("/api/answers")
                        .with(csrf())
                        .session(authenticatedSession(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyQuestionId": %d,
                                  "content": "수정된 답변"
                                }
                                """.formatted(dailyQuestion.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyQuestionId").value(dailyQuestion.getId()))
                .andExpect(jsonPath("$.content").value("수정된 답변"));

        org.assertj.core.api.Assertions.assertThat(
                        answerRepository.findByDailyQuestion_IdAndUser_Id(dailyQuestion.getId(), user.getId())
                )
                .isPresent()
                .get()
                .extracting(Answer::getContent)
                .isEqualTo("수정된 답변");
    }

    @Test
    @DisplayName("PUT /api/answers/{answerId} 는 본인 답변 수정만 허용한다")
    void updateAnswerRejectsOtherUsersAnswer() throws Exception {
        User me = createUser("me@example.com", "민지");
        User partner = createUser("partner@example.com", "도윤");
        Couple couple = createCoupleWithMembers(me, partner);
        Question question = questionRepository.saveAndFlush(new Question("오늘 질문", true, 1));
        DailyQuestion dailyQuestion = dailyQuestionRepository.saveAndFlush(
                new DailyQuestion(couple, question, LocalDate.of(2026, 4, 24))
        );
        Answer myAnswer = answerRepository.saveAndFlush(new Answer(dailyQuestion, me, "내 답변"));
        Answer partnerAnswer = answerRepository.saveAndFlush(new Answer(dailyQuestion, partner, "상대 답변"));

        mockMvc.perform(put("/api/answers/{answerId}", myAnswer.getId())
                        .with(csrf())
                        .session(authenticatedSession(me.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수정한 내 답변"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("수정한 내 답변"));

        mockMvc.perform(put("/api/answers/{answerId}", partnerAnswer.getId())
                        .with(csrf())
                        .session(authenticatedSession(me.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "남의 답변 수정 시도"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ANSWER_NOT_OWNED"));
    }

    private User createUser(String email, String displayName) {
        return userRepository.saveAndFlush(new User(email, passwordEncoder.encode("password123"), displayName));
    }

    private Couple createCoupleWithMember(User user) {
        Couple couple = coupleRepository.saveAndFlush(new Couple());
        coupleMemberRepository.saveAndFlush(new CoupleMember(couple, user, LocalDateTime.of(2026, 4, 24, 0, 0)));
        return couple;
    }

    private Couple createCoupleWithMembers(User first, User second) {
        Couple couple = coupleRepository.saveAndFlush(new Couple());
        coupleMemberRepository.saveAndFlush(new CoupleMember(couple, first, LocalDateTime.of(2026, 4, 24, 0, 0)));
        coupleMemberRepository.saveAndFlush(new CoupleMember(couple, second, LocalDateTime.of(2026, 4, 24, 0, 1)));
        return couple;
    }

    private MockHttpSession authenticatedSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_USER_ID, userId);
        return session;
    }
}
