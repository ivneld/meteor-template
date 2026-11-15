package com.meteor.domain.archfixture;

import jakarta.persistence.EntityManager;

/** R-05 (EntityManager 직접 사용), R-16 (바꾸고 저장하지 않음), R-20 (다른 애그리거트의 생성자 호출) 위반. */
public class FixtureOrderWriter {

    private final FixtureOrderRepository fixtureOrderRepository;

    private final EntityManager entityManager;

    public FixtureOrderWriter(FixtureOrderRepository fixtureOrderRepository, EntityManager entityManager) {
        this.fixtureOrderRepository = fixtureOrderRepository;
        this.entityManager = entityManager;
    }

    public FixtureOrder cancel(Long orderId) {
        FixtureOrder order = fixtureOrderRepository.findById(orderId).orElseThrow();
        order.cancel();
        return order;
    }

    public FixtureOrder place(Long memberId) {
        entityManager.flush();
        return fixtureOrderRepository.save(new FixtureOrder(FixtureMember.register(memberId)));
    }

}
