package app.service;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    public static final String SESSION_USER_ID = "userId";

    private final UserRepository userRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            CoupleMemberRepository coupleMemberRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.coupleMemberRepository = coupleMemberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request, HttpServletRequest servletRequest) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "이미 사용 중인 이메일입니다.");
        }

        User savedUser = userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName()
        ));

        HttpSession session = rotateSession(servletRequest);
        session.setAttribute(SESSION_USER_ID, savedUser.getId());

        return new SignupResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getDisplayName());
    }

    @Transactional
    public AuthUserResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        User user = userRepository.findByEmail(request.email())
                .filter(foundUser -> passwordEncoder.matches(request.password(), foundUser.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        HttpSession session = rotateSession(servletRequest);
        session.setAttribute(SESSION_USER_ID, user.getId());

        return toAuthUserResponse(user);
    }

    public AuthUserResponse getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));

        return toAuthUserResponse(user);
    }

    private AuthUserResponse toAuthUserResponse(User user) {
        Long coupleId = coupleMemberRepository.findByUser_Id(user.getId())
                .map(CoupleMember::getCouple)
                .map(couple -> couple.getId())
                .orElse(null);

        return new AuthUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), coupleId);
    }

    private HttpSession rotateSession(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession();
        servletRequest.changeSessionId();
        return session;
    }
}
