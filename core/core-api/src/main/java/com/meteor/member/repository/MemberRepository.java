package com.meteor.member.repository;

import java.util.Optional;

import com.meteor.member.domain.Email;
import com.meteor.member.domain.Member;
import com.meteor.member.domain.MemberStatus;
import com.meteor.member.storage.MemberEntity;
import com.meteor.member.storage.MemberJpaRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

/**
 * 회원 저장소 어댑터. storage 모듈의 JPA 엔티티를 이 모듈의 애그리거트로 바꾸고, 그 반대도 한다. 엔티티는 이 클래스 밖으로 나가지
 * 않는다(R-08).
 */
@Repository
public class MemberRepository {

    private final MemberJpaRepository jpa;

    MemberRepository(MemberJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Member> findById(Long id) {
        return jpa.findById(id).map(MemberRepository::toDomain);
    }

    public Member save(Member member) {
        MemberEntity entity;
        if (member.isNew()) {
            entity = new MemberEntity(member.getEmail().value(), member.getName(), member.getStatus().name());
        }
        else {
            entity = jpa.findById(member.getId())
                .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND).property("memberId", member.getId()));
            entity.changeStatus(member.getStatus().name());
        }
        return toDomain(jpa.save(entity));
    }

    private static Member toDomain(MemberEntity entity) {
        return Member.restore(entity.getId(), Email.of(entity.getEmail()), entity.getName(),
                MemberStatus.valueOf(entity.getStatus()));
    }

}
