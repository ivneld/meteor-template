package com.meteor.support.web;

import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import org.junit.jupiter.api.Test;

import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsTest {

    @Test
    void rendersStatusTypeTitleAndCode() {
        ProblemDetail problemDetail = ProblemDetails.of(ErrorCode.ORDER_NOT_FOUND, "orderId=1");

        assertThat(problemDetail.getStatus()).isEqualTo(404);
        assertThat(problemDetail.getType()).hasToString("urn:meteor:error:O001");
        assertThat(problemDetail.getTitle()).isEqualTo("Order not found.");
        assertThat(problemDetail.getDetail()).isEqualTo("orderId=1");
        assertThat(problemDetail.getProperties()).containsEntry("code", "O001");
    }

    @Test
    void fromStatusMapsFrameworkStatusesToCommonCodes() {
        assertThat(ProblemDetails.fromStatus(404)).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ProblemDetails.fromStatus(405)).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED);
        assertThat(ProblemDetails.fromStatus(415)).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        assertThat(ProblemDetails.fromStatus(409)).isEqualTo(ErrorCode.INVALID_REQUEST);
        assertThat(ProblemDetails.fromStatus(503)).isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    void coreExceptionPropertiesBecomeExtensions() {
        CoreException exception = new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", 7L);

        ProblemDetail problemDetail = ProblemDetails.of(exception);

        assertThat(problemDetail.getDetail()).isNull();
        assertThat(problemDetail.getProperties()).containsEntry("code", "O001").containsEntry("orderId", 7L);
    }

}
