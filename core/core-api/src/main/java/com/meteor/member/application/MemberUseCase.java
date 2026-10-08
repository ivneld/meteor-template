package com.meteor.member.application;

import com.meteor.member.domain.Member;
import com.meteor.member.storage.MemberRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Member 컨텍스트의 유스케이스. 트랜잭션 경계와 흐름만 맡고, 결과로는 애그리거트를 그대로 돌려준다. 규칙은 애그리거트와 *Policy 에 있다.
 */
@Service
public class MemberUseCase {

    private final MemberRepository memberRepository;

    public MemberUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Member register(MemberRegisterCommand command) {
        return memberRepository.save(Member.register(command.email(), command.name()));
    }

    @Transactional(readOnly = true)
    public Member find(Long memberId) {
        return load(memberId);
    }

    @Transactional
    public Member withdraw(Long memberId) {
        Member member = load(memberId);
        member.withdraw();
        return memberRepository.save(member);
    }

    private Member load(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND).property("memberId", memberId));
    }

}
