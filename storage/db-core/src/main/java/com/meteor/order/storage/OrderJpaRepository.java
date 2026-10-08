package com.meteor.order.storage;

import com.meteor.order.domain.OrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    long countByMemberIdAndStatus(Long memberId, OrderStatus status);

}
