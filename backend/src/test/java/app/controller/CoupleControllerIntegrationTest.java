package app.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.InviteCode;
import app.domain.User;
import app.repository.CoupleMemberRepository;
import app.repository.CoupleRepository;
import app.repository.InviteCodeRepository;
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
class CoupleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private InviteCodeRepository inviteCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/couples 는 로그인 사용자의 커플과 초대 코드를 생성한다")
    void createCoupleCreatesInviteCode() throws Exception {
        User user = userRepository.saveAndFlush(new User("create@example.com", passwordEncoder.encode("password123"), "민지"));
        MockHttpSession session = authenticatedSession(user.getId());

        mockMvc.perform(post("/api/couples").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupleId").isNumber())
                .andExpect(jsonPath("$.inviteCode").isString())
                .andExpect(jsonPath("$.expiresAt").isString());

        org.assertj.core.api.Assertions.assertThat(coupleRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(coupleMemberRepository.findByUser_Id(user.getId())).isPresent();
        org.assertj.core.api.Assertions.assertThat(inviteCodeRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("이미 커플에 속한 사용자는 커플 생성을 거부한다")
    void createCoupleRejectsAlreadyJoinedUser() throws Exception {
        User user = userRepository.saveAndFlush(new User("taken@example.com", passwordEncoder.encode("password123"), "민지"));
        Couple couple = coupleRepository.saveAndFlush(new Couple());
        coupleMemberRepository.saveAndFlush(new CoupleMember(couple, user, LocalDateTime.of(2026, 4, 24, 0, 0)));

        mockMvc.perform(post("/api/couples").with(csrf()).session(authenticatedSession(user.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_IN_COUPLE"));
    }

    @Test
    @DisplayName("POST /api/couples 는 미인증 요청에 401을 반환한다")
    void createCoupleRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/couples").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    @DisplayName("POST /api/couples/join 은 유효한 초대 코드로 참여를 완료한다")
    void joinCoupleSucceedsWithValidInviteCode() throws Exception {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        User owner = userRepository.saveAndFlush(new User("owner@example.com", passwordEncoder.encode("password123"), "민지"));
        User joiner = userRepository.saveAndFlush(new User("joiner@example.com", passwordEncoder.encode("password123"), "도윤"));
        Couple couple = coupleRepository.saveAndFlush(new Couple());
        coupleMemberRepository.saveAndFlush(new CoupleMember(couple, owner, LocalDateTime.of(2026, 4, 24, 0, 0)));
        inviteCodeRepository.saveAndFlush(new InviteCode(couple, "A1B2C3D4", nowUtc.plusHours(24)));

        mockMvc.perform(post("/api/couples/join")
                        .with(csrf())
                        .session(authenticatedSession(joiner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "A1B2C3D4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupleId").value(couple.getId()))
                .andExpect(jsonPath("$.memberCount").value(2));

        InviteCode inviteCode = inviteCodeRepository.findByCode("A1B2C3D4").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(coupleMemberRepository.findByUser_Id(joiner.getId()))
                .isPresent()
                .get()
                .extracting(member -> member.getCouple().getId())
                .isEqualTo(couple.getId());
        org.assertj.core.api.Assertions.assertThat(inviteCode.getUsedAt()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(inviteCode.getUsedByUser()).isNotNull();
    }

    @Test
    @DisplayName("만료, 사용됨, 없음 초대 코드는 INVITE_CODE_INVALID 를 반환한다")
    void joinCoupleRejectsInvalidInviteCodes() throws Exception {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        User owner = userRepository.saveAndFlush(new User("owner2@example.com", passwordEncoder.encode("password123"), "민지"));
        User joiner = userRepository.saveAndFlush(new User("joiner2@example.com", passwordEncoder.encode("password123"), "도윤"));
        Couple couple = coupleRepository.saveAndFlush(new Couple());
        coupleMemberRepository.saveAndFlush(new CoupleMember(couple, owner, LocalDateTime.of(2026, 4, 24, 0, 0)));
        InviteCode expired = inviteCodeRepository.saveAndFlush(new InviteCode(couple, "EXPIRED1", nowUtc.minusMinutes(1)));
        InviteCode used = inviteCodeRepository.saveAndFlush(new InviteCode(couple, "USED0001", nowUtc.plusHours(24)));
        used.markUsed(owner, nowUtc.minusMinutes(5));
        inviteCodeRepository.saveAndFlush(used);

        mockMvc.perform(post("/api/couples/join")
                        .with(csrf())
                        .session(authenticatedSession(joiner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "EXPIRED1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITE_CODE_INVALID"));

        mockMvc.perform(post("/api/couples/join")
                        .with(csrf())
                        .session(authenticatedSession(joiner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "USED0001"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITE_CODE_INVALID"));

        mockMvc.perform(post("/api/couples/join")
                        .with(csrf())
                        .session(authenticatedSession(joiner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "MISSING1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITE_CODE_INVALID"));

        org.assertj.core.api.Assertions.assertThat(expired.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("커플 정원이 가득 찼거나 이미 커플 소속이면 참여를 거부한다")
    void joinCoupleRejectsFullCoupleAndAlreadyJoinedUser() throws Exception {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        User owner = userRepository.saveAndFlush(new User("owner3@example.com", passwordEncoder.encode("password123"), "민지"));
        User partner = userRepository.saveAndFlush(new User("partner3@example.com", passwordEncoder.encode("password123"), "도윤"));
        User extra = userRepository.saveAndFlush(new User("extra3@example.com", passwordEncoder.encode("password123"), "지수"));
        User joinedUser = userRepository.saveAndFlush(new User("joined3@example.com", passwordEncoder.encode("password123"), "하늘"));

        Couple fullCouple = coupleRepository.saveAndFlush(new Couple());
        Couple anotherCouple = coupleRepository.saveAndFlush(new Couple());
        coupleMemberRepository.saveAndFlush(new CoupleMember(fullCouple, owner, LocalDateTime.of(2026, 4, 24, 0, 0)));
        coupleMemberRepository.saveAndFlush(new CoupleMember(fullCouple, partner, LocalDateTime.of(2026, 4, 24, 0, 1)));
        coupleMemberRepository.saveAndFlush(new CoupleMember(anotherCouple, joinedUser, LocalDateTime.of(2026, 4, 24, 0, 2)));
        inviteCodeRepository.saveAndFlush(new InviteCode(fullCouple, "FULL0001", nowUtc.plusHours(24)));

        mockMvc.perform(post("/api/couples/join")
                        .with(csrf())
                        .session(authenticatedSession(extra.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "FULL0001"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPLE_FULL"));

        mockMvc.perform(post("/api/couples/join")
                        .with(csrf())
                        .session(authenticatedSession(joinedUser.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "FULL0001"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_IN_COUPLE"));
    }

    @Test
    @DisplayName("POST /api/couples/join 은 미인증 요청에 401을 반환한다")
    void joinCoupleRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/couples/join")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "inviteCode": "A1B2C3D4"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    private MockHttpSession authenticatedSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthService.SESSION_USER_ID, userId);
        return session;
    }
}
