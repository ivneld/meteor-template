package com.meteor.member.storage;

import java.util.Optional;

import com.meteor.member.domain.Member;

/**
 * 회원 저장소. 도메인 객체만 주고받으며, 구현과 Spring Data 인터페이스는 package-private 이다.
 */
public interface MemberRepository {

    Optional<Member> findById(Long id);

    Member save(Member member);

}
