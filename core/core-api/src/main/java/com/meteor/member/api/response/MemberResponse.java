package com.meteor.member.api.response;

import com.meteor.member.domain.Member;
import com.meteor.member.enums.MemberStatus;

/**
 * 응답 DTO. 도메인 타입은 enum 만 쓰고 값 객체는 원시 타입으로 푼다(R-11). 애그리거트에서는 getter 만 읽는다(R-03).
 */
public record MemberResponse(Long id, String email, String name, MemberStatus status) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail().value(), member.getName(), member.getStatus());
    }

}
