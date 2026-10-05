package com.meteor.support.web;

import com.meteor.shared.error.CoreException;
import com.meteor.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsTest {

    @Test
    void rendersStatusTypeTitleAndCode() {
        ProblemDetail problemDetail = ProblemDetails.of(ErrorCode.SAMPLE_NOT_FOUND, "sampleId=1");

        assertThat(problemDetail.getStatus()).isEqualTo(404);
        assertThat(problemDetail.getType()).hasToString("urn:meteor:error:S001");
        assertThat(problemDetail.getTitle()).isEqualTo("Sample not found.");
        assertThat(problemDetail.getDetail()).isEqualTo("sampleId=1");
        assertThat(problemDetail.getProperties()).containsEntry("code", "S001");
    }

    @Test
    void coreExceptionPropertiesBecomeExtensions() {
        CoreException exception = new CoreException(ErrorCode.SAMPLE_NOT_FOUND).property("sampleId", 7L);

        ProblemDetail problemDetail = ProblemDetails.of(exception);

        assertThat(problemDetail.getDetail()).isNull();
        assertThat(problemDetail.getProperties()).containsEntry("code", "S001").containsEntry("sampleId", 7L);
    }

}
