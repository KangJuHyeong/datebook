package app.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExportControllerIntegrationTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("POST /api/exports 는 빈 항목, 접근 불가 항목, 잠금 항목을 거부한다")
    void createExportValidatesItems() throws Exception {
        User me = createUser("export-me@example.com", "민지");
        User partner = createUser("export-partner@example.com", "도윤");
        User stranger = createUser("export-stranger@example.com", "지수");
        Couple couple = createCoupleWithMembers(me, partner);
        Couple otherCouple = createCoupleWithMember(stranger);
        DailyQuestion lockedQuestion = createDailyQuestion(couple, "잠긴 질문", LocalDate.of(2026, 4, 23));
        DailyQuestion otherCoupleQuestion = createDailyQuestion(otherCouple, "다른 커플 질문", LocalDate.of(2026, 4, 22));
        answerRepository.saveAndFlush(new Answer(lockedQuestion, me, "내 답변만 있음"));

        mockMvc.perform(post("/api/exports")
                        .session(authenticatedSession(me.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyQuestionIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/exports")
                        .session(authenticatedSession(me.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyQuestionIds": [%d]
                                }
                                """.formatted(otherCoupleQuestion.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXPORT_ITEM_INVALID"));

        mockMvc.perform(post("/api/exports")
                        .session(authenticatedSession(me.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyQuestionIds": [%d]
                                }
                                """.formatted(lockedQuestion.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXPORT_ITEM_INVALID"));
    }

    @Test
    @DisplayName("REQUESTED 에서 PREVIEWED, COMPLETED 로 진행하고 완료 후 다운로드와 스냅샷 유지가 동작한다")
    void exportLifecycleAndSnapshotArePreserved() throws Exception {
        User me = createUser("lifecycle-me@example.com", "민지");
        User partner = createUser("lifecycle-partner@example.com", "도윤");
        Couple couple = createCoupleWithMembers(me, partner);
        DailyQuestion first = createDailyQuestion(couple, "첫 질문", LocalDate.of(2026, 4, 22));
        DailyQuestion second = createDailyQuestion(couple, "둘째 질문", LocalDate.of(2026, 4, 23));
        answerRepository.saveAndFlush(new Answer(first, me, "첫 내 답변"));
        answerRepository.saveAndFlush(new Answer(first, partner, "첫 상대 답변"));
        Answer mySecondAnswer = answerRepository.saveAndFlush(new Answer(second, me, "둘째 내 답변"));
        answerRepository.saveAndFlush(new Answer(second, partner, "둘째 상대 답변"));

        MvcResult createResult = mockMvc.perform(post("/api/exports")
                        .session(authenticatedSession(me.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyQuestionIds": [%d, %d]
                                }
                                """.formatted(second.getId(), first.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn();

        Long exportRequestId = extractExportRequestId(createResult);

        mockMvc.perform(post("/api/exports/{exportRequestId}/preview", exportRequestId)
                        .session(authenticatedSession(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREVIEWED"))
                .andExpect(jsonPath("$.entries[0].date").value("2026-04-22"))
                .andExpect(jsonPath("$.entries[1].date").value("2026-04-23"));

        mockMvc.perform(get("/api/exports/{exportRequestId}/download", exportRequestId)
                        .session(authenticatedSession(me.getId()))
                        .param("format", "json"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EXPORT_NOT_COMPLETED"));

        mockMvc.perform(post("/api/exports/{exportRequestId}/complete", exportRequestId)
                        .session(authenticatedSession(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.downloads[0].format").value("json"))
                .andExpect(jsonPath("$.downloads[1].format").value("text"));

        String jsonBeforeUpdate = mockMvc.perform(get("/api/exports/{exportRequestId}/download", exportRequestId)
                        .session(authenticatedSession(me.getId()))
                        .param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"couple-diary-%d.json\"".formatted(exportRequestId)))
                .andExpect(jsonPath("$.entries[0].date").value("2026-04-22"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String textBeforeUpdate = mockMvc.perform(get("/api/exports/{exportRequestId}/download", exportRequestId)
                        .session(authenticatedSession(me.getId()))
                        .param("format", "text"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"couple-diary-%d.txt\"".formatted(exportRequestId)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("둘째 내 답변")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        mockMvc.perform(put("/api/answers/{answerId}", mySecondAnswer.getId())
                        .session(authenticatedSession(me.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "완료 후 수정한 답변"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/exports/{exportRequestId}/download", exportRequestId)
                        .session(authenticatedSession(me.getId()))
                        .param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(content().json(jsonBeforeUpdate));

        mockMvc.perform(get("/api/exports/{exportRequestId}/download", exportRequestId)
                        .session(authenticatedSession(me.getId()))
                        .param("format", "text"))
                .andExpect(status().isOk())
                .andExpect(content().string(textBeforeUpdate));
    }

    @Test
    @DisplayName("POST /api/exports/{id}/cancel 은 REQUESTED 주문을 취소한다")
    void cancelExportSucceedsFromRequested() throws Exception {
        User me = createUser("cancel-me@example.com", "민지");
        User partner = createUser("cancel-partner@example.com", "도윤");
        Couple couple = createCoupleWithMembers(me, partner);
        DailyQuestion dailyQuestion = createDailyQuestion(couple, "질문", LocalDate.of(2026, 4, 24));
        answerRepository.saveAndFlush(new Answer(dailyQuestion, me, "내 답변"));
        answerRepository.saveAndFlush(new Answer(dailyQuestion, partner, "상대 답변"));

        MvcResult createResult = mockMvc.perform(post("/api/exports")
                        .session(authenticatedSession(me.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyQuestionIds": [%d]
                                }
                                """.formatted(dailyQuestion.getId())))
                .andExpect(status().isOk())
                .andReturn();

        Long exportRequestId = extractExportRequestId(createResult);

        mockMvc.perform(post("/api/exports/{exportRequestId}/cancel", exportRequestId)
                        .session(authenticatedSession(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
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

    private DailyQuestion createDailyQuestion(Couple couple, String content, LocalDate date) {
        Question question = questionRepository.saveAndFlush(new Question(content, true, 1));
        return dailyQuestionRepository.saveAndFlush(new DailyQuestion(couple, question, date));
    }

    private MockHttpSession authenticatedSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_USER_ID, userId);
        return session;
    }

    private Long extractExportRequestId(MvcResult mvcResult) throws Exception {
        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return root.get("exportRequestId").asLong();
    }
}
