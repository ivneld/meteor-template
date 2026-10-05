package com.meteor.member.application;

import com.meteor.member.domain.Member;
import com.meteor.member.storage.MemberRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 컨텍스트가 다른 컨텍스트에 공개하는 유일한 동기 진입점. 도메인 객체를 돌려주지 않고 질문에만 답한다.
 *
 * <p>
 * 서비스로 분리될 때는 이 클래스를 인터페이스로 추출하고 HTTP 클라이언트 구현으로 교체한다.
 */
@Component
public class MemberFacade {

    private final MemberRepository memberRepository;

    public MemberFacade(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public void ensureActive(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND).property("memberId", memberId));
        if (!member.isActive()) {
            throw new CoreException(ErrorCode.MEMBER_NOT_ACTIVE).property("memberId", memberId);
        }
    }

}
