package app.common.error;

import java.util.List;

public record ErrorResponse(String code, String message, List<FieldErrorResponse> fields) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }
}
