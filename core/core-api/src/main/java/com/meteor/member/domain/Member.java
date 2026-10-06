package com.meteor.member.domain;

import com.meteor.shared.Email;
import com.meteor.shared.MemberStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

/**
 * 회원 애그리거트. 다른 컨텍스트는 이 객체를 알지 못하고 memberId 로만 참조한다.
 */
public class Member {

    private final Long id;

    private final Email email;

    private final String name;

    private MemberStatus status;

    private Member(Long id, Email email, String name, MemberStatus status) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.status = status;
    }

    public static Member register(Email email, String name) {
        return new Member(null, email, name, MemberStatus.ACTIVE);
    }

    /** 저장소에서 복원한다. storage 어댑터만 호출한다. */
    public static Member restore(Long id, Email email, String name, MemberStatus status) {
        return new Member(id, email, name, status);
    }

    public void withdraw() {
        if (status == MemberStatus.WITHDRAWN) {
            throw new CoreException(ErrorCode.MEMBER_ALREADY_WITHDRAWN).property("memberId", id);
        }
        status = MemberStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public MemberStatus getStatus() {
        return status;
    }

}
