package com.meteor.order.storage;

import com.meteor.order.enums.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    long countByMemberIdAndStatus(Long memberId, OrderStatus status);

}
