package com.meteor.shipping.application;

import com.meteor.order.application.OrderPaidEvent;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문이 결제 완료되면 배송을 준비한다. 주문 컨텍스트는 배송의 존재를 모른다.
 */
@Component
public class ShippingEventListener {

    private final ShippingUseCase shippingUseCase;

    public ShippingEventListener(ShippingUseCase shippingUseCase) {
        this.shippingUseCase = shippingUseCase;
    }

    @TransactionalEventListener
    public void on(OrderPaidEvent event) {
        shippingUseCase.prepare(event.orderId(), event.shippingAddress());
    }

}
