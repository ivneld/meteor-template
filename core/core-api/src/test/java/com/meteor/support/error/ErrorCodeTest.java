package com.meteor.support.error;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.meteor.support.error.ErrorCode;

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
    void coreExceptionRejectsReservedPropertyNames() {
        CoreException exception = new CoreException(ErrorCode.ORDER_NOT_FOUND);

        assertThatThrownBy(() -> exception.property("status", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exception.property("detail", "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coreExceptionKeepsCodeDetailAndProperties() {
        CoreException exception = new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", 7L);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        assertThat(exception.getDetail()).isNull();
        assertThat(exception.getMessage()).isEqualTo("Order not found.");
        assertThat(exception.getProperties()).containsEntry("orderId", 7L);
    }

}
