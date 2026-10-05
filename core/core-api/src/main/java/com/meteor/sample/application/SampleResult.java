package com.meteor.sample.application;

import java.time.LocalDateTime;

import com.meteor.sample.domain.Sample;
import com.meteor.shared.SampleStatus;

/**
 * UseCase 의 출력. 애그리거트를 api 계층에 노출하지 않기 위한 평면 구조.
 */
public record SampleResult(Long id, String name, SampleStatus status, LocalDateTime createdAt) {

    static SampleResult from(Sample sample) {
        return new SampleResult(sample.getId(), sample.getName(), sample.getStatus(), sample.getCreatedAt());
    }

}
