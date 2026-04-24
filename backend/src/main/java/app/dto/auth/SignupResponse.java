package app.dto.auth;

public record SignupResponse(
        Long id,
        String email,
        String displayName
) {
}
