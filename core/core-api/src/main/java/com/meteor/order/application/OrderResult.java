package com.meteor.order.application;

import com.meteor.order.domain.Order;
import com.meteor.shared.Address;
import com.meteor.shared.Money;
import com.meteor.shared.OrderStatus;

public record OrderResult(Long id, Long memberId, String productName, int quantity, Money unitPrice, Money totalAmount,
        Address shippingAddress, OrderStatus status) {

    static OrderResult from(Order order) {
        return new OrderResult(order.getId(), order.getMemberId(), order.getProductName(), order.getQuantity(),
                order.getUnitPrice(), order.totalAmount(), order.getShippingAddress(), order.getStatus());
    }

}
