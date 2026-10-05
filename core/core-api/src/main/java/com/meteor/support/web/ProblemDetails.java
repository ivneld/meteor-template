package com.meteor.support.web;

import java.net.URI;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.http.ProblemDetail;

/**
 * {@link ErrorCode} 를 RFC 9457 ProblemDetail 로 렌더링한다. 에러 코드는 어휘(core-domain),
 * ProblemDetail 은 표현(api)이다.
 */
public final class ProblemDetails {

    /** type URI 접두어. 에러 코드 문서 URL 이 생기면 그 주소로 바꾼다. */
    private static final String TYPE_PREFIX = "urn:meteor:error:";

    private ProblemDetails() {
    }

    public static ProblemDetail of(ErrorCode errorCode, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(errorCode.getStatus());
        problemDetail.setDetail(detail);
        return apply(errorCode, problemDetail);
    }

    public static ProblemDetail of(CoreException exception) {
        ProblemDetail problemDetail = of(exception.getErrorCode(), exception.getDetail());
        exception.getProperties().forEach(problemDetail::setProperty);
        return problemDetail;
    }

    /** 이미 만들어진 ProblemDetail 에 에러 코드의 type / title / code 를 덧씌운다. */
    public static ProblemDetail apply(ErrorCode errorCode, ProblemDetail problemDetail) {
        problemDetail.setType(URI.create(TYPE_PREFIX + errorCode.getCode()));
        problemDetail.setTitle(errorCode.getTitle());
        problemDetail.setProperty("code", errorCode.getCode());
        return problemDetail;
    }

    /**
     * Spring MVC 가 직접 만들어 내는 표준 오류(404, 405, 415 ...)에 대응하는 공통 코드를 고른다.
     */
    public static ErrorCode fromStatus(int status) {
        if (status == ErrorCode.NOT_FOUND.getStatus()) {
            return ErrorCode.NOT_FOUND;
        }
        if (status == ErrorCode.METHOD_NOT_ALLOWED.getStatus()) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (status == ErrorCode.UNSUPPORTED_MEDIA_TYPE.getStatus()) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }
        if (status >= 400 && status < 500) {
            return ErrorCode.INVALID_REQUEST;
        }
        return ErrorCode.INTERNAL_ERROR;
    }

    public static boolean hasCode(ProblemDetail problemDetail) {
        return problemDetail.getProperties() != null && problemDetail.getProperties().containsKey("code");
    }

}
