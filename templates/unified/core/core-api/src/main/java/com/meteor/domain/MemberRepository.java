package com.meteor.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 저장소. 애그리거트가 곧 엔티티이므로 Spring Data 인터페이스를 그대로 쓴다. 이 인터페이스를 부르는 것은 {@link MemberReader}
 * 와 {@link MemberWriter} 뿐이다(R-04, R-09, R-10).
 *
 * <p>
 * 더티 체킹에 기대지 않는다. 애그리거트를 바꿨으면 save 를 명시적으로 호출한다(R-16). 영속성 컨텍스트 안의 엔티티라 save 는 거의 아무 일도
 * 하지 않지만, 저장 시점이 코드에 보이고 엔티티와 애그리거트를 나누는 구조로 옮길 때 고칠 곳이 없다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

}
