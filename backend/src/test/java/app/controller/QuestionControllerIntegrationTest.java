package app.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuestionControllerIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T00:00:00Z");
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

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

    @MockBean
    @Qualifier("utcClock")
    private Clock utcClock;

    @MockBean
    @Qualifier("seoulClock")
    private Clock seoulClock;

    @BeforeEach
    void setUpClocks() {
        when(utcClock.instant()).thenReturn(FIXED_INSTANT);
        when(utcClock.getZone()).thenReturn(ZoneOffset.UTC);
        when(seoulClock.instant()).thenReturn(FIXED_INSTANT);
        when(seoulClock.getZone()).thenReturn(SEOUL_ZONE);
    }

    @Test
    @DisplayName("GET /api/questions/today 는 같은 커플 같은 날짜에 같은 질문을 반환한다")
    void getTodayQuestionReturnsSameQuestionForSameDate() throws Exception {
        User user = createUser("same-date@example.com", "민지");
        Couple couple = createCoupleWithMember(user);
        Question question = questionRepository.saveAndFlush(new Question("오늘 질문", true, 1));
        DailyQuestion dailyQuestion = dailyQuestionRepository.saveAndFlush(
                new DailyQuestion(couple, question, LocalDate.of(2026, 4, 24))
        );

        mockMvc.perform(get("/api/questions/today").session(authenticatedSession(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyQuestionId").value(dailyQuestion.getId()))
                .andExpect(jsonPath("$.question").value("오늘 질문"));

        mockMvc.perform(get("/api/questions/today").session(authenticatedSession(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyQuestionId").value(dailyQuestion.getId()));
    }

    @Test
    @DisplayName("GET /api/questions/today 는 질문 순환 배정 규칙을 따른다")
    void getTodayQuestionAssignsByRotation() throws Exception {
        User user = createUser("rotation@example.com", "민지");
        Couple couple = createCoupleWithMember(user);
        Question first = questionRepository.saveAndFlush(new Question("질문 1", true, 1));
        Question second = questionRepository.saveAndFlush(new Question("질문 2", true, 2));
        Question third = questionRepository.saveAndFlush(new Question("질문 3", true, 3));

        dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, first, LocalDate.of(2026, 4, 20)));
        dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, second, LocalDate.of(2026, 4, 21)));
        dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, third, LocalDate.of(2026, 4, 22)));
        dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, first, LocalDate.of(2026, 4, 23)));

        mockMvc.perform(get("/api/questions/today").session(authenticatedSession(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("질문 2"));
    }

    @Test
    @DisplayName("잠금 상태에서는 partnerAnswer content가 응답에 포함되지 않는다")
    void getTodayQuestionHidesLockedPartnerContent() throws Exception {
        User me = createUser("locked-me@example.com", "민지");
        User partner = createUser("locked-partner@example.com", "도윤");
        Couple couple = createCoupleWithMembers(me, partner);
        Question question = questionRepository.saveAndFlush(new Question("오늘 질문", true, 1));
        DailyQuestion dailyQuestion = dailyQuestionRepository.saveAndFlush(
                new DailyQuestion(couple, question, LocalDate.of(2026, 4, 24))
        );
        answerRepository.saveAndFlush(new Answer(dailyQuestion, partner, "상대만 답변"));

        mockMvc.perform(get("/api/questions/today").session(authenticatedSession(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerState").value("PARTNER_ANSWERED_ME_WAITING"))
                .andExpect(jsonPath("$.partnerAnswer.status").value("ANSWERED_LOCKED"))
                .andExpect(jsonPath("$.partnerAnswer.content").doesNotExist());
    }

    @Test
    @DisplayName("둘 다 답하면 공개된 partnerAnswer content를 응답한다")
    void getTodayQuestionRevealsPartnerContentWhenBothAnswered() throws Exception {
        User me = createUser("reveal-me@example.com", "민지");
        User partner = createUser("reveal-partner@example.com", "도윤");
        Couple couple = createCoupleWithMembers(me, partner);
        Question question = questionRepository.saveAndFlush(new Question("오늘 질문", true, 1));
        DailyQuestion dailyQuestion = dailyQuestionRepository.saveAndFlush(
                new DailyQuestion(couple, question, LocalDate.of(2026, 4, 24))
        );
        answerRepository.saveAndFlush(new Answer(dailyQuestion, me, "내 답변"));
        answerRepository.saveAndFlush(new Answer(dailyQuestion, partner, "상대 답변"));

        mockMvc.perform(get("/api/questions/today").session(authenticatedSession(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerState").value("BOTH_ANSWERED"))
                .andExpect(jsonPath("$.partnerAnswer.status").value("REVEALED"))
                .andExpect(jsonPath("$.partnerAnswer.content").value("상대 답변"))
                .andExpect(jsonPath("$.isFullyAnswered").value(true));
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
