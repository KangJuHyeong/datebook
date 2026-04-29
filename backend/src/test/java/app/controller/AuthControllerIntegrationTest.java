package app.controller;

import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.User;
import app.repository.CoupleMemberRepository;
import app.repository.CoupleRepository;
import app.repository.UserRepository;
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
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private CoupleMemberRepository coupleMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공 시 세션이 생성되고 사용자 정보를 반환한다")
    void signupCreatesSession() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "signup@example.com",
                                  "password": "password123",
                                  "displayName": "민지"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("signup@example.com"))
                .andExpect(jsonPath("$.displayName").value("민지"))
                .andExpect(request().sessionAttribute("userId", org.hamcrest.Matchers.notNullValue()));

        User savedUser = userRepository.findByEmail("signup@example.com").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(savedUser.getPasswordHash()).isNotEqualTo("password123");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("password123", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("회원가입은 중복 이메일을 거부한다")
    void signupRejectsDuplicateEmail() throws Exception {
        userRepository.saveAndFlush(new User("duplicate@example.com", "hash", "기존"));

        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "duplicate@example.com",
                                  "password": "password123",
                                  "displayName": "민지"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    @DisplayName("회원가입 validation 오류는 필드 정보와 함께 반환한다")
    void signupValidatesRequestFields() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "short",
                                  "displayName": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields").isArray())
                .andExpect(jsonPath("$.fields").isNotEmpty());
    }

    @Test
    @DisplayName("로그인 성공 시 coupleId를 포함한 현재 사용자 정보를 반환한다")
    void loginReturnsUserInfo() throws Exception {
        User user = userRepository.saveAndFlush(new User("login@example.com", passwordEncoder.encode("password123"), "민지"));
        Couple couple = coupleRepository.saveAndFlush(new Couple());
        coupleMemberRepository.saveAndFlush(new CoupleMember(couple, user, java.time.LocalDateTime.now()));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.displayName").value("민지"))
                .andExpect(jsonPath("$.coupleId").value(couple.getId()))
                .andExpect(request().sessionAttribute("userId", user.getId()));
    }

    @Test
    @DisplayName("로그인 실패는 이메일 존재 여부를 드러내지 않는다")
    void loginFailureUsesGenericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호를 확인해주세요."))
                .andExpect(jsonPath("$.fields", empty()));
    }

    @Test
    @DisplayName("현재 사용자 조회는 로그인된 세션에서만 동작한다")
    void meRequiresAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    @DisplayName("현재 사용자 조회는 세션의 userId로 사용자 정보를 반환한다")
    void meReturnsCurrentUser() throws Exception {
        User user = userRepository.saveAndFlush(new User("me@example.com", passwordEncoder.encode("password123"), "민지"));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.coupleId").isEmpty());
    }

    @Test
    @DisplayName("CSRF 토큰이 없는 상태 변경 요청은 공통 JSON 오류를 반환한다")
    void unsafeRequestsRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.fields", empty()));
    }

    @Test
    @DisplayName("CSRF 토큰 조회는 헤더 이름과 토큰을 반환한다")
    void csrfReturnsToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("로그아웃은 세션을 무효화하고 성공 응답을 반환한다")
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        org.assertj.core.api.Assertions.assertThat(session.isInvalid()).isTrue();
    }

    @Test
    @DisplayName("CORS는 localhost:3000 credentials 요청을 허용한다")
    void corsAllowsFrontendOriginWithCredentials() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
