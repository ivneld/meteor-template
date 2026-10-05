package com.meteor.member.storage;

import java.util.Optional;

import com.meteor.member.domain.Member;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

@Repository
class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJpaRepository jpa;

    MemberRepositoryAdapter(MemberJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpa.findById(id).map(MemberEntity::toDomain);
    }

    @Override
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
