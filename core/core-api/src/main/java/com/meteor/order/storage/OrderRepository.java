package com.meteor.order.storage;

import java.util.Optional;

import com.meteor.order.domain.Order;
import com.meteor.order.domain.OrderStatus;

public interface OrderRepository {

    Optional<Order> findById(Long id);

    Order save(Order order);

    long countByMemberIdAndStatus(Long memberId, OrderStatus status);

}
