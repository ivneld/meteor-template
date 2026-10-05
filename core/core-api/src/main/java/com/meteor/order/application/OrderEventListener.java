package com.meteor.order.application;

import com.meteor.payment.application.PaymentCompletedEvent;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 컨텍스트의 이벤트를 받아 주문 상태를 바꾼다. 커밋 이후(AFTER_COMMIT, 기본값)에만 동작한다(S-07).
 */
@Component
public class OrderEventListener {

    private final OrderUseCase orderUseCase;

    public OrderEventListener(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @TransactionalEventListener
    public void on(PaymentCompletedEvent event) {
        orderUseCase.markPaid(event.orderId());
    }

}
