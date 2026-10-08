package com.meteor.member.application;

import com.meteor.member.domain.Member;
import com.meteor.member.repository.MemberRepository;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 컨텍스트가 다른 컨텍스트에 공개하는 유일한 동기 진입점. 도메인 객체나 상태 값을 돌려주지 않고, 판단 결과만 알린다(R-10, S-10).
 *
 * <p>
 * 판단 자체는 애그리거트({@link Member#ensureActive()})에 있고 Facade 는 꺼내서 묻기만 한다. 별도 애플리케이션이 회원 규칙을
 * 필요로 하게 되면 이 클래스의 메서드가 그대로 공개 API 스펙이 된다. 규칙을 그쪽에 다시 구현하지 않는다.
 */
@Component
public class MemberFacade {

    private final MemberRepository memberRepository;

    public MemberFacade(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public void ensureActive(Long memberId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> new CoreException(ErrorCode.MEMBER_NOT_FOUND).property("memberId", memberId))
            .ensureActive();
    }

}
