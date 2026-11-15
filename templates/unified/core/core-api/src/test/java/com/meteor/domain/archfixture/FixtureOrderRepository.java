package com.meteor.domain.archfixture;

import java.util.Optional;

/**
 * 저장소 fixture. Spring Data 의 Repository 를 상속하지 않는다. 상속하면 테스트 컨텍스트가 이 인터페이스를 리포지토리로 등록하려다
 * 실패한다.
 */
public interface FixtureOrderRepository {

    Optional<FixtureOrder> findById(Long id);

    FixtureOrder save(FixtureOrder order);

}
