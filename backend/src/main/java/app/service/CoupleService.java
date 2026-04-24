package app.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CoupleService {

    private static final int MAX_COUPLE_MEMBERS = 2;
    private static final int INVITE_CODE_LENGTH = 12;

    private final UserRepository userRepository;
    private final CoupleRepository coupleRepository;
    private final CoupleMemberRepository coupleMemberRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final AppTimeProvider appTimeProvider;

    public CoupleService(
            UserRepository userRepository,
            CoupleRepository coupleRepository,
            CoupleMemberRepository coupleMemberRepository,
            InviteCodeRepository inviteCodeRepository,
            AppTimeProvider appTimeProvider
    ) {
        this.userRepository = userRepository;
        this.coupleRepository = coupleRepository;
        this.coupleMemberRepository = coupleMemberRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.appTimeProvider = appTimeProvider;
    }

    @Transactional
    public CreateCoupleResponse createCouple(HttpSession session) {
        User user = getAuthenticatedUser(session);
        ensureUserNotInCouple(user.getId());

        Couple couple = coupleRepository.save(new Couple());
        LocalDateTime now = appTimeProvider.nowUtcDateTime();
        coupleMemberRepository.save(new CoupleMember(couple, user, now));

        InviteCode inviteCode = inviteCodeRepository.save(
                new InviteCode(couple, generateInviteCode(), now.plusHours(24))
        );

        return new CreateCoupleResponse(couple.getId(), inviteCode.getCode(), inviteCode.getExpiresAt());
    }

    @Transactional
    public JoinCoupleResponse joinCouple(JoinCoupleRequest request, HttpSession session) {
        User user = getAuthenticatedUser(session);
        ensureUserNotInCouple(user.getId());

        InviteCode inviteCode = inviteCodeRepository.findByCode(normalizeInviteCode(request.inviteCode()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_INVALID));

        LocalDateTime now = appTimeProvider.nowUtcDateTime();
        if (inviteCode.isUsed() || inviteCode.isExpired(now)) {
            throw new BusinessException(ErrorCode.INVITE_CODE_INVALID);
        }

        long memberCount = coupleMemberRepository.countByCouple_Id(inviteCode.getCouple().getId());
        if (memberCount >= MAX_COUPLE_MEMBERS) {
            throw new BusinessException(ErrorCode.COUPLE_FULL);
        }

        coupleMemberRepository.save(new CoupleMember(inviteCode.getCouple(), user, now));
        inviteCode.markUsed(user, now);

        return new JoinCoupleResponse(inviteCode.getCouple().getId(), memberCount + 1);
    }

    private User getAuthenticatedUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(AuthService.SESSION_USER_ID);
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTH_REQUIRED);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));
    }

    private void ensureUserNotInCouple(Long userId) {
        if (coupleMemberRepository.existsByUser_Id(userId)) {
            throw new BusinessException(ErrorCode.ALREADY_IN_COUPLE);
        }
    }

    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, INVITE_CODE_LENGTH)
                    .toUpperCase(Locale.ROOT);
            if (inviteCodeRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.CONFIGURATION_ERROR);
    }

    private String normalizeInviteCode(String inviteCode) {
        return inviteCode.trim().toUpperCase(Locale.ROOT);
    }
}
