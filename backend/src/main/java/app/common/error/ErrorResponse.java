package app.common.error;

import java.util.List;

public record ErrorResponse(String code, String message, List<FieldErrorResponse> fields) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorResponse> fields) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.copyOf(fields));
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of());
    }
}
