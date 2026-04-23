package app.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorResponseTest {

    @Test
    void createsErrorResponseWithoutFields() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);

        assertThat(response.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.fields()).isEmpty();
    }
}
