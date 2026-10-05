package com.meteor.support.error;

/**
 * 서비스가 클라이언트에 돌려주는 모든 에러의 목록.
 *
 * <p>
 * 에러 하나는 <b>코드 문자열 + HTTP 상태 + 제목 + 로그 레벨</b>로 정의한다. 어떤 HTTP 상태로 응답할지는 예외를 던지는 쪽이 아니라 에러
 * 코드가 결정한다. ProblemDetail 로 바꾸는 일은 표현 계층(core-api 의 ProblemDetails)이 맡는다.
 *
 * <p>
 * 코드 문자열은 접두어로 영역을 구분한다. {@code C}: 공통, {@code S}: 샘플 컨텍스트. 컨텍스트가 늘어나면 접두어를 추가한다.
 */
public enum ErrorCode {

    // ----- 공통 (C) -----
    INVALID_REQUEST("C001", 400, "Invalid request.", LogLevel.INFO),
    NOT_FOUND("C002", 404, "Resource not found.", LogLevel.INFO),
    METHOD_NOT_ALLOWED("C003", 405, "Method not allowed.", LogLevel.INFO),
    UNSUPPORTED_MEDIA_TYPE("C004", 415, "Unsupported media type.", LogLevel.INFO),
    INTERNAL_ERROR("C999", 500, "An unexpected error has occurred.", LogLevel.ERROR),

    // ----- 샘플 컨텍스트 (S) -----
    SAMPLE_NOT_FOUND("S001", 404, "Sample not found.", LogLevel.INFO),
    SAMPLE_ALREADY_INACTIVE("S002", 409, "Sample is already inactive.", LogLevel.INFO);

    private final String code;

    private final int status;

    private final String title;

    private final LogLevel logLevel;

    ErrorCode(String code, int status, String title, LogLevel logLevel) {
        this.code = code;
        this.status = status;
        this.title = title;
        this.logLevel = logLevel;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

}
