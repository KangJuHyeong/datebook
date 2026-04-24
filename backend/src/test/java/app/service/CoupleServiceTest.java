package app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import app.common.error.BusinessException;
import app.common.error.ErrorCode;
import app.common.time.AppTimeProvider;
import app.domain.Couple;
import app.domain.CoupleMember;
import app.domain.InviteCode;
import app.domain.User;
import app.dto.couple.CreateCoupleResponse;
import app.dto.couple.JoinCoupleRequest;
import app.dto.couple.JoinCoupleResponse;
import app.repository.CoupleMemberRepository;
import app.repository.CoupleRepository;
import app.repository.InviteCodeRepository;
import app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CoupleServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-24T02:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private CoupleRepository coupleRepository;

    @Mock
    private CoupleMemberRepository coupleMemberRepository;

    @Mock
    private InviteCodeRepository inviteCodeRepository;

    @Mock
    private HttpSession session;

    @Spy
    private AppTimeProvider appTimeProvider = new AppTimeProvider(
            Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC),
            Clock.fixed(FIXED_INSTANT, ZoneId.of("Asia/Seoul"))
    );

    @InjectMocks
    private CoupleService coupleService;

    @Test
    @DisplayName("로그인 사용자는 커플을 생성하고 24시간 유효한 초대 코드를 발급받는다")
    void createCoupleCreatesMemberAndInviteCode() {
        User user = new User("user@example.com", "hash", "민지");
        Couple couple = new Couple();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(couple, "id", 10L);
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(coupleMemberRepository.existsByUser_Id(1L)).thenReturn(false);
        when(coupleRepository.save(any(Couple.class))).thenReturn(couple);
        when(inviteCodeRepository.findByCode(any(String.class))).thenReturn(Optional.empty());
        when(inviteCodeRepository.save(any(InviteCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCoupleResponse response = coupleService.createCouple(session);

        assertThat(response.coupleId()).isEqualTo(10L);
        assertThat(response.inviteCode()).hasSize(12);
        assertThat(response.expiresAt()).isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).plusHours(24));

        ArgumentCaptor<CoupleMember> memberCaptor = ArgumentCaptor.forClass(CoupleMember.class);
        verify(coupleMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getCouple()).isEqualTo(couple);
        assertThat(memberCaptor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("이미 커플에 속한 사용자는 새 커플을 만들 수 없다")
    void createCoupleRejectsUsersAlreadyInCouple() {
        User user = new User("user@example.com", "hash", "민지");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(coupleMemberRepository.existsByUser_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> coupleService.createCouple(session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALREADY_IN_COUPLE);
    }

    @Test
    @DisplayName("유효한 초대 코드로 커플 참여에 성공한다")
    void joinCoupleUsesInviteCode() {
        User user = new User("join@example.com", "hash", "서준");
        Couple couple = new Couple();
        InviteCode inviteCode = new InviteCode(couple, "ABC123DEF456", LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).plusHours(24));
        ReflectionTestUtils.setField(user, "id", 2L);
        ReflectionTestUtils.setField(couple, "id", 20L);
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(coupleMemberRepository.existsByUser_Id(2L)).thenReturn(false);
        when(inviteCodeRepository.findByCode("ABC123DEF456")).thenReturn(Optional.of(inviteCode));
        when(coupleMemberRepository.countByCouple_Id(20L)).thenReturn(1L);

        JoinCoupleResponse response = coupleService.joinCouple(new JoinCoupleRequest("abc123def456"), session);

        assertThat(response.coupleId()).isEqualTo(20L);
        assertThat(response.memberCount()).isEqualTo(2L);
        assertThat(inviteCode.isUsed()).isTrue();
        assertThat(inviteCode.getUsedByUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("만료되었거나 사용된 초대 코드는 거절한다")
    void joinCoupleRejectsInvalidInviteCode() {
        User user = new User("join@example.com", "hash", "서준");
        Couple couple = new Couple();
        InviteCode inviteCode = new InviteCode(couple, "ABC123DEF456", LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).minusMinutes(1));
        ReflectionTestUtils.setField(user, "id", 2L);
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(coupleMemberRepository.existsByUser_Id(2L)).thenReturn(false);
        when(inviteCodeRepository.findByCode("ABC123DEF456")).thenReturn(Optional.of(inviteCode));

        assertThatThrownBy(() -> coupleService.joinCouple(new JoinCoupleRequest("ABC123DEF456"), session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVITE_CODE_INVALID);
    }

    @Test
    @DisplayName("정원이 찬 커플에는 참여할 수 없다")
    void joinCoupleRejectsFullCouple() {
        User user = new User("join@example.com", "hash", "서준");
        Couple couple = new Couple();
        InviteCode inviteCode = new InviteCode(couple, "ABC123DEF456", LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).plusHours(24));
        ReflectionTestUtils.setField(user, "id", 2L);
        ReflectionTestUtils.setField(couple, "id", 20L);
        when(session.getAttribute(AuthService.SESSION_USER_ID)).thenReturn(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(coupleMemberRepository.existsByUser_Id(2L)).thenReturn(false);
        when(inviteCodeRepository.findByCode("ABC123DEF456")).thenReturn(Optional.of(inviteCode));
        when(coupleMemberRepository.countByCouple_Id(20L)).thenReturn(2L);

        assertThatThrownBy(() -> coupleService.joinCouple(new JoinCoupleRequest("ABC123DEF456"), session))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPLE_FULL);
    }
}
