package com.meteor.sample.api.request;

import com.meteor.sample.application.SampleCreateCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 요청 DTO 는 형식 검증과 Command 변환만 담당한다.
 */
public record SampleCreateRequest(@NotBlank String name) {

    public SampleCreateCommand toCommand() {
        return new SampleCreateCommand(name);
    }

}
