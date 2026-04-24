package app.dto.auth;

public record AuthUserResponse(
        Long id,
        String email,
        String displayName,
        Long coupleId
) {
}
