package com.meteor.order.storage;

import java.util.Optional;

import com.meteor.order.domain.Order;
import com.meteor.order.domain.OrderStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

@Repository
class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpa;

    OrderRepositoryAdapter(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return jpa.findById(id).map(OrderEntity::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity;
        if (order.isNew()) {
            entity = OrderEntity.from(order);
        }
        else {
            entity = jpa.findById(order.getId())
                .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", order.getId()));
            entity.apply(order);
        }
        return jpa.save(entity).toDomain();
    }

    @Override
    public long countByMemberIdAndStatus(Long memberId, OrderStatus status) {
        return jpa.countByMemberIdAndStatus(memberId, status);
    }

}
