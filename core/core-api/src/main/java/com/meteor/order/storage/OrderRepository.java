package com.meteor.order.storage;

import java.util.Optional;

import com.meteor.order.domain.Order;

public interface OrderRepository {

    Optional<Order> findById(Long id);

    Order save(Order order);

}
