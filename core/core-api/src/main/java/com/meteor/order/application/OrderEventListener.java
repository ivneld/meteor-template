package com.meteor.order.application;

import com.meteor.payment.application.PaymentCompletedEvent;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 결제 컨텍스트의 이벤트를 받아 주문 상태를 바꾼다.
 *
 * <p>
 * 결제 승인과 주문 PAID 는 함께 바뀌어야 하므로 <b>결제 트랜잭션 안에서 동기로</b> 받는다(S-05). 주문 쪽이 실패하면 결제도 롤백된다. 결제
 * 코드는 여전히 주문을 모르고 이벤트만 발행한다. 코드 결합은 느슨하게, 데이터 일관성은 강하게 둔다.
 *
 * <p>
 * 이 리스너는 "같은 트랜잭션으로 묶인 컨텍스트 간 협력" 목록에 들어간다(ARCHITECTURE.md 측정). 이 흐름을 다른 애플리케이션과 나눠야 하는
 * 날이 오면 이 목록이 비동기·보상 처리로 바꿀 대상이다.
 */
@Component
public class OrderEventListener {

    private final OrderUseCase orderUseCase;

    public OrderEventListener(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @EventListener
    public void on(PaymentCompletedEvent event) {
        orderUseCase.markPaid(event.orderId());
    }

}
