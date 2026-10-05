package com.meteor.support.error;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void codeStringsAreUnique() {
        Set<String> codes = new HashSet<>();
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(codes.add(errorCode.getCode())).as("duplicated code: %s", errorCode.getCode()).isTrue();
        }
    }

    @Test
    void everyCodeHasHttpStatusInErrorRange() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertThat(errorCode.getStatus()).as(errorCode.name()).isBetween(400, 599);
        }
    }

    @Test
    void coreExceptionKeepsCodeDetailAndProperties() {
        CoreException exception = new CoreException(ErrorCode.SAMPLE_NOT_FOUND).property("sampleId", 7L);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SAMPLE_NOT_FOUND);
        assertThat(exception.getDetail()).isNull();
        assertThat(exception.getMessage()).isEqualTo("Sample not found.");
        assertThat(exception.getProperties()).containsEntry("sampleId", 7L);
    }

}
