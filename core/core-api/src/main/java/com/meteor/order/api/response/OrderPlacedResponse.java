package com.meteor.order.api.response;

import com.meteor.order.application.OrderPlacementResult;

public record OrderPlacedResponse(OrderResponse order, long remainingAwaitingPaymentOrders) {

    public static OrderPlacedResponse from(OrderPlacementResult result) {
        return new OrderPlacedResponse(OrderResponse.from(result.order()), result.remainingAwaitingPaymentOrders());
    }

}
