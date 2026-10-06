package com.meteor.member.storage;

import com.meteor.member.domain.Member;
import com.meteor.shared.Email;
import com.meteor.shared.MemberStatus;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "member")
class MemberEntity extends BaseEntity {

    @Column(nullable = false, length = 200, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    protected MemberEntity() {
    }

    static MemberEntity from(Member member) {
        MemberEntity entity = new MemberEntity();
        entity.email = member.getEmail().value();
        entity.name = member.getName();
        entity.status = member.getStatus();
        return entity;
    }

    void apply(Member member) {
        this.status = member.getStatus();
    }

    Member toDomain() {
        return Member.restore(getId(), Email.of(email), name, status);
    }

}
