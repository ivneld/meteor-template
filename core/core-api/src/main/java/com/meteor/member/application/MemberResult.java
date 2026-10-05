package com.meteor.member.application;

import com.meteor.member.domain.Member;
import com.meteor.shared.Email;
import com.meteor.shared.MemberStatus;

public record MemberResult(Long id, Email email, String name, MemberStatus status) {

    static MemberResult from(Member member) {
        return new MemberResult(member.getId(), member.getEmail(), member.getName(), member.getStatus());
    }

}
