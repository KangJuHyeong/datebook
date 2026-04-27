package app.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DiaryControllerIntegrationTest {

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
    @DisplayName("GET /api/diary 는 날짜 내림차순과 잠금 상태를 반환한다")
    void getDiaryReturnsDescendingEntries() throws Exception {
        User me = createUser("diary-me@example.com", "민지");
        User partner = createUser("diary-partner@example.com", "도윤");
        Couple couple = createCoupleWithMembers(me, partner);
        DailyQuestion older = createDailyQuestion(couple, "질문 1", LocalDate.of(2026, 4, 23));
        DailyQuestion newer = createDailyQuestion(couple, "질문 2", LocalDate.of(2026, 4, 24));
        answerRepository.saveAndFlush(new Answer(older, me, "내 답변"));
        answerRepository.saveAndFlush(new Answer(older, partner, "상대 답변"));
        answerRepository.saveAndFlush(new Answer(newer, partner, "상대만 답변"));

        mockMvc.perform(get("/api/diary").session(authenticatedSession(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].dailyQuestionId").value(newer.getId()))
                .andExpect(jsonPath("$.entries[0].partnerAnswerStatus").value("ANSWERED_LOCKED"))
                .andExpect(jsonPath("$.entries[0].exportable").value(false))
                .andExpect(jsonPath("$.entries[0].partnerAnswer").doesNotExist())
                .andExpect(jsonPath("$.entries[1].dailyQuestionId").value(older.getId()))
                .andExpect(jsonPath("$.entries[1].partnerAnswerStatus").value("REVEALED"))
                .andExpect(jsonPath("$.entries[1].myAnswer.displayName").value("민지"))
                .andExpect(jsonPath("$.entries[1].myAnswer.content").value("내 답변"))
                .andExpect(jsonPath("$.entries[1].partnerAnswer.displayName").value("도윤"))
                .andExpect(jsonPath("$.entries[1].partnerAnswer.content").value("상대 답변"))
                .andExpect(jsonPath("$.entries[1].exportable").value(true));
    }

    @Test
    @DisplayName("GET /api/diary 는 인증이 없으면 401을 반환한다")
    void getDiaryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/diary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    private User createUser(String email, String displayName) {
        return userRepository.saveAndFlush(new User(email, passwordEncoder.encode("password123"), displayName));
    }

    private Couple createCoupleWithMembers(User first, User second) {
        Couple couple = coupleRepository.saveAndFlush(new Couple());
        coupleMemberRepository.saveAndFlush(new CoupleMember(couple, first, LocalDateTime.of(2026, 4, 24, 0, 0)));
        coupleMemberRepository.saveAndFlush(new CoupleMember(couple, second, LocalDateTime.of(2026, 4, 24, 0, 1)));
        return couple;
    }

    private DailyQuestion createDailyQuestion(Couple couple, String content, LocalDate date) {
        Question question = questionRepository.saveAndFlush(new Question(content, true, 1));
        return dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, question, date));
    }

    private MockHttpSession authenticatedSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_USER_ID, userId);
        return session;
    }
}
