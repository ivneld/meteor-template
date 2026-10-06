package com.meteor.member.api.response;

import com.meteor.member.application.MemberResult;
import com.meteor.member.domain.MemberStatus;

public record MemberResponse(Long id, String email, String name, MemberStatus status) {

    public static MemberResponse from(MemberResult result) {
        return new MemberResponse(result.id(), result.email().value(), result.name(), result.status());
    }

}
