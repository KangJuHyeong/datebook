package app.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호를 확인해주세요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근할 수 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인해주세요."),
    INVITE_CODE_INVALID(HttpStatus.BAD_REQUEST, "초대 코드를 확인해주세요."),
    COUPLE_FULL(HttpStatus.CONFLICT, "커플 정원이 가득 찼습니다."),
    ALREADY_IN_COUPLE(HttpStatus.CONFLICT, "이미 커플에 속해 있습니다."),
    DAILY_QUESTION_CONFLICT(HttpStatus.CONFLICT, "오늘 질문을 다시 불러와주세요."),
    ANSWER_LOCKED(HttpStatus.FORBIDDEN, "아직 공개되지 않은 답변입니다."),
    ANSWER_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 답변만 수정할 수 있습니다."),
    EXPORT_ITEM_INVALID(HttpStatus.BAD_REQUEST, "주문할 기록을 다시 확인해주세요."),
    EXPORT_STATE_INVALID(HttpStatus.CONFLICT, "주문 상태를 다시 확인해주세요."),
    EXPORT_NOT_COMPLETED(HttpStatus.CONFLICT, "주문 완료 후 다운로드할 수 있습니다."),
    EXPORT_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "지원하지 않는 다운로드 형식입니다."),
    CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 설정을 확인해주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
