package com.meteor.order.storage;

import java.util.Optional;

import com.meteor.order.domain.Order;
import com.meteor.order.domain.OrderStatus;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Repository;

/**
 * 주문 저장소. 도메인 객체만 주고받고, JPA 엔티티와의 변환은 이 클래스 안에서 끝난다. 엔티티와 Spring Data 인터페이스는
 * package-private 이라 밖에서 보이지 않는다(R-08).
 *
 * <p>
 * 더티 체킹에 기대지 않는다. UseCase 가 도메인 객체를 바꿨으면 {@link #save(Order)} 를 명시적으로 호출해야 한다(S-09).
 */
@Repository
public class OrderRepository {

    private final OrderJpaRepository jpa;

    OrderRepository(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    public Optional<Order> findById(Long id) {
        return jpa.findById(id).map(OrderEntity::toDomain);
    }

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

    public long countByMemberIdAndStatus(Long memberId, OrderStatus status) {
        return jpa.countByMemberIdAndStatus(memberId, status);
    }

}
