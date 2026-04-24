package app.dto.couple;

import java.time.LocalDateTime;

public record CreateCoupleResponse(
        Long coupleId,
        String inviteCode,
        LocalDateTime expiresAt
) {
}
