package com.meteor.member.api.request;

import com.meteor.member.application.MemberRegisterCommand;
import com.meteor.member.domain.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 형식 검증은 Bean Validation, 값 검증은 VO 생성자가 한다. Email 생성이 실패하면 400 으로 응답된다.
 */
public record MemberRegisterRequest(@NotBlank String email, @NotBlank String name) {

    public MemberRegisterCommand toCommand() {
        return new MemberRegisterCommand(Email.of(email), name);
    }

}
