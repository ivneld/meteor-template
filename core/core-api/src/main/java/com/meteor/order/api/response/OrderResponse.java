package com.meteor.order.api.response;

import com.meteor.order.application.OrderResult;
import com.meteor.shared.Address;
import com.meteor.shared.OrderStatus;

public record OrderResponse(Long id, Long memberId, String productName, int quantity, long unitPrice, long totalAmount,
        Address shippingAddress, OrderStatus status) {

    public static OrderResponse from(OrderResult result) {
        return new OrderResponse(result.id(), result.memberId(), result.productName(), result.quantity(),
                result.unitPrice().amount(), result.totalAmount().amount(), result.shippingAddress(), result.status());
    }

}
