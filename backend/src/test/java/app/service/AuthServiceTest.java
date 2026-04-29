package app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.User;
import app.dto.auth.AuthUserResponse;
import app.dto.auth.LoginRequest;
import app.dto.auth.SignupRequest;
import app.dto.auth.SignupResponse;
import app.repository.CoupleMemberRepository;
import app.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CoupleMemberRepository coupleMemberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입은 비밀번호를 해시하고 세션에는 userId만 저장한다")
    void signupEncodesPasswordAndStoresOnlyUserIdInSession() {
        SignupRequest request = new SignupRequest("user@example.com", "password123", "민지");
        User persistedUser = new User("user@example.com", "encoded-password", "민지");
        ReflectionTestUtils.setField(persistedUser, "id", 1L);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(persistedUser);
        when(servletRequest.getSession()).thenReturn(session);

        SignupResponse response = authService.signup(request, servletRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        verify(servletRequest).changeSessionId();
        verify(session).setAttribute(AuthService.SESSION_USER_ID, 1L);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.displayName()).isEqualTo("민지");
    }

    @Test
    @DisplayName("중복 이메일 회원가입은 validation error를 반환한다")
    void signupRejectsDuplicateEmail() {
        SignupRequest request = new SignupRequest("user@example.com", "password123", "민지");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User("user@example.com", "hash", "기존")));

        assertThatThrownBy(() -> authService.signup(request, servletRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인은 비밀번호 일치 시 사용자와 coupleId를 반환한다")
    void loginReturnsAuthenticatedUser() {
        User user = new User("user@example.com", "stored-hash", "민지");
        Couple couple = new Couple();
        ReflectionTestUtils.setField(user, "id", 2L);
        ReflectionTestUtils.setField(couple, "id", 10L);
        CoupleMember coupleMember = new CoupleMember(couple, user, java.time.LocalDateTime.now());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "stored-hash")).thenReturn(true);
        when(coupleMemberRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(coupleMember));
        when(servletRequest.getSession()).thenReturn(session);

        AuthUserResponse response = authService.login(new LoginRequest("user@example.com", "password123"), servletRequest);

        verify(servletRequest).changeSessionId();
        verify(session).setAttribute(AuthService.SESSION_USER_ID, user.getId());
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.coupleId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("로그인 실패는 이메일 존재 여부를 숨긴다")
    void loginRejectsInvalidCredentials() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "password123"), servletRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }
}
