package com.meteor.sample.domain;

import com.meteor.shared.SampleStatus;
import com.meteor.shared.error.CoreException;
import com.meteor.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 도메인 테스트는 Spring 없이 돈다. 규칙이 애그리거트에 있기 때문이다.
 */
class SampleTest {

    @Test
    void createdSampleIsActiveAndNew() {
        Sample sample = Sample.create("meteor");

        assertThat(sample.isNew()).isTrue();
        assertThat(sample.getStatus()).isEqualTo(SampleStatus.ACTIVE);
        assertThat(sample.getCreatedAt()).isNotNull();
    }

    @Test
    void deactivateChangesStatus() {
        Sample sample = Sample.create("meteor");

        sample.deactivate();

        assertThat(sample.getStatus()).isEqualTo(SampleStatus.INACTIVE);
    }

    @Test
    void deactivateTwiceIsRejected() {
        Sample sample = Sample.restore(1L, "meteor", SampleStatus.INACTIVE, null);

        assertThatThrownBy(sample::deactivate).isInstanceOf(CoreException.class)
            .satisfies(
                    e -> assertThat(((CoreException) e).getErrorCode()).isEqualTo(ErrorCode.SAMPLE_ALREADY_INACTIVE));
    }

}
