package com.meteor.domain;

import com.meteor.enums.MemberStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 회원 애그리거트이자 JPA 엔티티. 다른 애그리거트는 이 객체를 필드로 갖지 않고 memberId 로만 참조한다(R-14).
 *
 * <p>
 * JPA 가 요구하는 기본 생성자는 protected 로 두고 코드에서 부르지 않는다(R-20). setter 는 두지 않는다(R-15). 상태는 규칙을 가진
 * 메서드로만 바뀌고, 바꾼 뒤에는 {@link MemberWriter} 가 save 로 저장한다(R-16).
 */
@Entity
@Table(name = "member")
public class Member extends BaseEntity {

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, length = 200, unique = true))
    private Email email;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    protected Member() {
    }

    private Member(Email email, String name, MemberStatus status) {
        this.email = email;
        this.name = name;
        this.status = status;
    }

    public static Member register(Email email, String name) {
        return new Member(email, name, MemberStatus.ACTIVE);
    }

    public void withdraw() {
        if (status == MemberStatus.WITHDRAWN) {
            throw new CoreException(ErrorCode.MEMBER_ALREADY_WITHDRAWN).property("memberId", getId());
        }
        status = MemberStatus.WITHDRAWN;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    /** "이 회원으로 무언가를 해도 되는가". 판단은 회원이 하고 거절은 예외로 알린다. */
    public void ensureActive() {
        if (!isActive()) {
            throw new CoreException(ErrorCode.MEMBER_NOT_ACTIVE).property("memberId", getId());
        }
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
