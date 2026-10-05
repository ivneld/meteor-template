package com.meteor.support.web;

import java.net.URI;

import com.meteor.shared.error.CoreException;
import com.meteor.shared.error.ErrorCode;

import org.springframework.http.ProblemDetail;

/**
 * {@link ErrorCode} 를 RFC 9457 ProblemDetail 로 렌더링한다. 에러 코드는 어휘(core-shared),
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

    public static boolean hasCode(ProblemDetail problemDetail) {
        return problemDetail.getProperties() != null && problemDetail.getProperties().containsKey("code");
    }

}
