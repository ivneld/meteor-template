package com.meteor.member.application;

import com.meteor.member.domain.Member;
import com.meteor.member.storage.MemberRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberUseCase {

    private final MemberRepository memberRepository;

    public MemberUseCase(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public MemberResult register(MemberRegisterCommand command) {
        return MemberResult.from(memberRepository.save(Member.register(command.email(), command.name())));
    }

    @Transactional(readOnly = true)
    public MemberResult find(Long memberId) {
        return MemberResult.from(load(memberId));
    }

    @Transactional
    public MemberResult withdraw(Long memberId) {
        Member member = load(memberId);
        member.withdraw();
        return MemberResult.from(memberRepository.save(member));
    }

    private Member load(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND).property("memberId", memberId));
    }

}
