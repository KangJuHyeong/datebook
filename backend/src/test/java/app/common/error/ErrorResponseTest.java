package app.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorResponseTest {

    @Test
    void createsErrorResponseWithoutFields() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);

        assertThat(response.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.message()).isEqualTo("잠시 후 다시 시도해주세요.");
        assertThat(response.fields()).isEmpty();
    }

    @Test
    void createsErrorResponseWithFields() {
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                List.of(new FieldErrorResponse("email", "올바른 이메일 형식이 아닙니다."))
        );

        assertThat(response.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.fields()).hasSize(1);
    }

    @Test
    void exposesExpectedErrorCodeStatusMapping() {
        assertThat(ErrorCode.AUTH_REQUIRED.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.EXPORT_STATE_INVALID.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.CONFIGURATION_ERROR.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
