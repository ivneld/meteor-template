package com.meteor.member.storage;

import com.meteor.support.storage.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 회원 영속 모델. 도메인을 모르므로 상태는 문자열로 저장하고, 도메인 객체와의 변환은 core 모듈의 MemberRepository 가 한다.
 */
@Entity
@Table(name = "member")
public class MemberEntity extends BaseEntity {

    @Column(nullable = false, length = 200, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String status;

    protected MemberEntity() {
    }

    public MemberEntity(String email, String name, String status) {
        this.email = email;
        this.name = name;
        this.status = status;
    }

    public void changeStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

}
