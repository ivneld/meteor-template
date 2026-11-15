package com.meteor.domain.archfixture;

import java.util.Optional;

import com.meteor.storage.OrderEntity;

/** R-05 위반: 엔티티를 그대로 돌려준다. */
public class FixtureOrderRepository {

    public Optional<FixtureOrder> findById(Long id) {
        return Optional.empty();
    }

    public FixtureOrder save(FixtureOrder order) {
        return order;
    }

    public OrderEntity findEntity(Long id) {
        return null;
    }

}
