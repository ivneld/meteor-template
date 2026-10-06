package com.meteor.order.application;

import com.meteor.order.storage.OrderRepository;
import com.meteor.shared.Money;
import com.meteor.support.error.CoreException;
import com.meteor.support.error.ErrorCode;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 컨텍스트가 결제 컨텍스트에 공개하는 동기 진입점. 판단은 주문이 하고 결과(Money)만 돌려준다. 반환 타입은 shared 의 값이라 상대 컨텍스트가
 * 주문 모델을 알 필요가 없다.
 */
@Component
public class OrderFacade {

    private final OrderRepository orderRepository;

    public OrderFacade(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Money payableAmount(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND).property("orderId", orderId))
            .payableAmount();
    }

}
