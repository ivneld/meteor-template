package com.meteor.shipping.application;

import com.meteor.order.application.OrderPaidEvent;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문이 결제 완료되면 배송을 준비한다. 주문 컨텍스트는 배송의 존재를 모른다.
 *
 * <p>
 * 배송 준비는 결제·주문이 확정된 <b>뒤의</b> 부가 처리다. 배송 쪽 실패로 결제를 되돌리지 않으므로 커밋 이후(AFTER_COMMIT, 기본값)에
 * 받는다(S-07). 여기서 난 예외는 호출자에게 전파되지 않고 로그로만 남으므로, 실패를 감지하고 다시 처리하는 수단(로그 알림, 미생성 배송 점검)을 함께
 * 둔다. 함께 바뀌어야 하는 협력은 {@code OrderEventListener} 처럼 같은 트랜잭션으로 받는다.
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
