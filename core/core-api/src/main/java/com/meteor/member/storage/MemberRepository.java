package com.meteor.member.storage;

import java.util.Optional;

import com.meteor.member.domain.Member;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

/**
 * 회원 저장소. 도메인 객체만 주고받고, JPA 엔티티와의 변환은 이 클래스 안에서 끝난다. 엔티티와 Spring Data 인터페이스는
 * package-private 이라 밖에서 보이지 않는다(R-08).
 */
@Repository
public class MemberRepository {

    private final MemberJpaRepository jpa;

    MemberRepository(MemberJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Member> findById(Long id) {
        return jpa.findById(id).map(MemberEntity::toDomain);
    }

    public Member save(Member member) {
        MemberEntity entity;
        if (member.isNew()) {
            entity = MemberEntity.from(member);
        }
        else {
            entity = jpa.findById(member.getId())
                .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND).property("memberId", member.getId()));
            entity.apply(member);
        }
        return jpa.save(entity).toDomain();
    }

}
