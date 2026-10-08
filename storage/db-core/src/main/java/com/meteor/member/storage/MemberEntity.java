package com.meteor.member.storage;

import com.meteor.member.enums.MemberStatus;
import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * 회원 영속 모델. 도메인을 모르고 core-enum 의 enum 만 안다. 도메인 객체와의 변환은 core 모듈의 MemberRepository 가 한다.
 */
@Entity
@Table(name = "member")
public class MemberEntity extends BaseEntity {

    @Column(nullable = false, length = 200, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    protected MemberEntity() {
    }

    public MemberEntity(String email, String name, MemberStatus status) {
        this.email = email;
        this.name = name;
        this.status = status;
    }

    public void changeStatus(MemberStatus status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public MemberStatus getStatus() {
        return status;
    }

}
