package com.meteor.sample.api.response;

import java.time.LocalDateTime;

import com.meteor.sample.application.SampleResult;
import com.meteor.shared.SampleStatus;

public record SampleResponse(Long id, String name, SampleStatus status, LocalDateTime createdAt) {

    public static SampleResponse from(SampleResult result) {
        return new SampleResponse(result.id(), result.name(), result.status(), result.createdAt());
    }

}
